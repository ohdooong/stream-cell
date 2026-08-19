package com.streamcell.platform.pipeline.service.impl;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.global._common.file.dto.FileResponse;
import com.streamcell.global._common.file.service.FileService;
import com.streamcell.platform._common.port.UserLookupPort;
import com.streamcell.platform.flink.client.FlinkRestClient;
import com.streamcell.platform.flink.dto.FlinkResponse;
import com.streamcell.platform.flink.dto.FlinkResponse.JobExceptionsEntry;
import com.streamcell.platform.flink.dto.FlinkResponse.JobExceptionsHistory;
import com.streamcell.platform.flink.enums.FlinkJobStatus;
import com.streamcell.platform.pipeline.converter.PipelineConverter;
import com.streamcell.platform.pipeline.domain.JobStatusConvertPolicy;
import com.streamcell.platform.pipeline.dto.PipelineResponse.PipelineStatus.Failure;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.service.PipelineDeploymentService;
import com.streamcell.platform.pipeline.validator.PipelineValidator;
import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.enums.ArtifactType;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.service.PipelineService;
import com.streamcell.platform.pipeline.vo.CustomJobConfig;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import com.streamcell.platform.pipeline.vo.PipelineDeployment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository repository;
    private final UserLookupPort userLookupPort;
    private final FileService fileService;

    private final JobStatusConvertPolicy jobStatusConvertPolicy;

    private final FlinkRestClient flinkRestClient;

    private final Map<String, PipelineValidator<?, ?>> validatorMap;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineResponse.Pipeline create(PipelineRequest.Create createItem) {
        // 사용자 검증
        validateUser(createItem.getOwnerUserId());

        Pipeline pipeline = PipelineConverter.toVO(createItem);
        repository.insert(pipeline);
        return PipelineConverter.toDTO(pipeline);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineResponse.Pipeline update(PipelineRequest.Update updateItem) {
        // 사용자 검증
        validateUser(updateItem.getOwnerUserId());

        Pipeline pipeline = PipelineConverter.toVO(updateItem);
        repository.update(pipeline);
        return PipelineConverter.toDTO(pipeline);
    }

    @Override
    public PipelineResponse.Pipeline findPipelineByPipelineId(Long pipelineId) {
        return repository.findPipelineByPipelineId(pipelineId)
                .map(PipelineConverter::toDTO)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineResponse.Artifact createFlinkCustomJar(
            MultipartFile file,
            PipelineRequest.CreateCustomJobConfig createCustomJobConfig,
            Long pipelineId) {

        // 파이프라인 id 검증
        repository.findPipelineByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));
        // user 검증
        validateUser(createCustomJobConfig.getUserId());

        // pipeline artifact와 custom job config가 존재하면 실패
        repository.findPipelineArtifactByPipelineId(pipelineId)
                    .ifPresent(artifact -> {
                        throw new BaseAPIException(ErrorCode.CONFLICT_PIPLINE_ARTIFACT);
                    });

        repository.findCustomJobConfigByPipelineId(pipelineId)
                .ifPresent(artifact -> {
                    throw new BaseAPIException(ErrorCode.CONFLICT_CUSTOM_JOB_CONFIG);
                });

        CustomJobConfig customJobConfig = PipelineConverter.toVO(createCustomJobConfig, pipelineId);

        // customJobConfig 유효성검증
        PipelineValidator<CustomJobConfig, Void> customJobConfigValidator =
                (PipelineValidator<CustomJobConfig, Void>) validatorMap.get("customJobConfigValidator");

        if (customJobConfigValidator == null) {
            throw new RuntimeException("validator Bean을 찾을 수 없습니다.");
        }
        customJobConfigValidator.validate(customJobConfig);

        // artifact job config 저장
        insertCustomJobConfig(customJobConfig);
        // 파일저장
        FileResponse.FileUpload uploaded = fileService.saveCustomJar(file, pipelineId);
        // artifact 메타데이터 저장
        PipelineArtifact artifact = insertPipelineArtifact(pipelineId, uploaded);

        repository.updatePipelineStatus(Pipeline.builder()
                .pipelineId(pipelineId)
                .pipelineStatus(PipelineStatus.ARTIFACT_UPLOADED)
                .build());

        return PipelineConverter.toDTO(artifact);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineResponse.PipelineStatus updatePipelineStatus(Long pipelineId) {

        Pipeline pipeline = repository.findPipelineByPipelineId(pipelineId)
            .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));

        PipelineDeployment pipelineDeployment = repository.findLatestPipelineDeployMentByPipelineId(pipelineId)
            .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE_DEPLOYMENT));

        FlinkJobStatus jobStatus = flinkRestClient.getJobStatus(pipelineDeployment.getFlinkJobId());

        PipelineStatus convertedStatus = jobStatusConvertPolicy.convertToPipelineStatusFrom(jobStatus);
        pipeline.setPipelineStatus(convertedStatus);

        DeploymentStatus deploymentStatus = jobStatusConvertPolicy.convertToDeploymentStatusFrom(jobStatus);
        pipelineDeployment.setStatus(deploymentStatus);

        LocalDateTime now = LocalDateTime.now();
        if (DeploymentStatus.STOPPED == deploymentStatus
                || DeploymentStatus.STOPPING == deploymentStatus) {
            pipelineDeployment.setStoppedAt(now);
            pipelineDeployment.setFinishedAt(now);
        }
        pipelineDeployment.setLastCheckedAt(now);

        if (DeploymentStatus.FAILED == deploymentStatus
            && pipelineDeployment.getErrorMessage() == null
            && pipelineDeployment.getErrorExceptionName() == null) {

            try {
                JobExceptionsHistory jobExceptions =
                    flinkRestClient.getExceptionsByJobId(pipelineDeployment.getFlinkJobId());

                JobExceptionsEntry rootExceptionEntry = jobExceptions.getExceptionEntries().get(0);

                pipelineDeployment.setErrorExceptionName(rootExceptionEntry.getExceptionName());
                pipelineDeployment.setErrorMessage(rootExceptionEntry.getStacktrace());
                pipelineDeployment.setErrorTimestamp(rootExceptionEntry.getTimestamp());

                repository.updatePipelineDeploymentError(pipelineDeployment);

            } catch (Exception e) {
                pipeline.setPipelineStatus(PipelineStatus.FAILED);
                pipelineDeployment.setStatus(DeploymentStatus.FAILED);
                log.error(e.getMessage());
            }
        }

        repository.updatePipelineStatus(pipeline);
        repository.updatePipelineDeploymentStatus(pipelineDeployment);

        return PipelineResponse.PipelineStatus
                .builder()
                .pipelineId(pipeline.getPipelineId())
                .deploymentId(pipelineDeployment.getDeploymentId())
                .flinkJobId(pipelineDeployment.getFlinkJobId())
                .deploymentStatus(pipelineDeployment.getStatus())
                .pipelineStatus(pipeline.getPipelineStatus())
                .failure(
                    pipelineDeployment.getErrorMessage() != null ?
                    PipelineResponse.PipelineStatus.Failure.from(
                        pipelineDeployment.getErrorExceptionName(),
                        pipelineDeployment.getErrorMessage(),
                        pipelineDeployment.getErrorTimestamp()
                    ) : null)
                .build();
    }


    private PipelineArtifact insertPipelineArtifact(Long pipelineId, FileResponse.FileUpload uploaded) {
        PipelineArtifact artifact = PipelineArtifact.builder()
                .pipelineId(pipelineId)
                .artifactType(ArtifactType.CUSTOM_JAR)
                .originalFileName(uploaded.getOriginalFileName())
                .storedFileName(uploaded.getSavedFileName())
                .storedFilePath(uploaded.getSavedPath())
                .build();
        repository.insertPipelineArtifact(artifact);
        return artifact;
    }

    private CustomJobConfig insertCustomJobConfig(CustomJobConfig customJobConfig) {
        repository.insertCustomJobConfig(customJobConfig);
        return customJobConfig;
    }

    private void validateUser(Long userId) {
        boolean isExistsUser = userLookupPort.existsByUserId(userId);
        if (!isExistsUser) {
            throw new BaseAPIException(ErrorCode.INVALID_USER);
        }
    }

}
