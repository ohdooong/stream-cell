package com.streamcell.platform.pipeline.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.global._common.file.dto.FileResponse;
import com.streamcell.global._common.file.service.FileService;
import com.streamcell.platform._common.port.UserLookupPort;
import com.streamcell.platform.ai.domain.context.FlinkSQLGenerationContext;
import com.streamcell.platform.ai.domain.context.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.generator.FlinkSQLGenerator;
import com.streamcell.platform.ai.domain.generator.FlinkSQLPreviewGenerator;
import com.streamcell.platform.ai.dto.AIDeploymentResponse;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.service.AIDeploymentService;
import com.streamcell.platform.flink.client.FlinkRestClient;
import com.streamcell.platform.flink.dto.FlinkResponse;
import com.streamcell.platform.flink.dto.FlinkResponse.JobException.JobExceptionsHistory.JobExceptionsEntry;
import com.streamcell.platform.flink.enums.FlinkJobStatus;
import com.streamcell.platform.pipeline.converter.PipelineConverter;
import com.streamcell.platform.pipeline.converter.PipelineDeploymentConverter;
import com.streamcell.platform.pipeline.domain.policy.JobStatusConvertPolicy;
import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.enums.ArtifactType;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.service.PipelineService;
import com.streamcell.platform.pipeline.validator.PipelineValidator;
import com.streamcell.platform.pipeline.vo.*;
import com.streamcell.platform.topic.converter.TopicConverter;
import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.platform.topic.service.TopicService;
import com.streamcell.platform.topic.vo.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository repository;
    private final UserLookupPort userLookupPort;

    // service
    private final FileService fileService;
    private final TopicService topicService;
    private final AIDeploymentService aiDeploymentService;

    // flink
    private final FlinkRestClient flinkRestClient;

    // generator
    private final FlinkSQLGenerator flinkSQLGenerator;
    private final FlinkSQLPreviewGenerator flinkSQLPreviewGenerator;

    // policy
    private final JobStatusConvertPolicy jobStatusConvertPolicy;

    private final PipelineConverter pipelineConverter;
    private final TopicConverter topicConverter;
    private final PipelineDeploymentConverter pipelineDeploymentConverter;
    private final Map<String, PipelineValidator<?, ?>> validatorMap;

    private final JsonMapper jsonMapper = new JsonMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineResponse.Pipeline create(PipelineRequest.Create createItem) {
        // 사용자 검증
        validateUser(createItem.getOwnerUserId());

        Pipeline pipeline = pipelineConverter.toVO(createItem);
        repository.insert(pipeline);
        return pipelineConverter.toDTO(pipeline);
    }

    @Override
    public PipelineResponse.Pipeline createAISqlConfig(Long pipelineId, PipelineRequest.CreateAISqlConfig createAISqlConfig) {

        // 파이프라인 id 검증
        Pipeline pipeline = repository.findPipelineByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));

        if (PipelineType.AI_SQL != pipeline.getPipelineType()) {
            throw new BaseAPIException(ErrorCode.INVALID_AI_SQL_REQUEST);
        }

        // user 토픽권한 검증
        Long userId = createAISqlConfig.getUserId();
        Long ownerUserId = pipeline.getOwnerUserId();

        if (!userId.equals(ownerUserId)) {
            throw new BaseAPIException(ErrorCode.FORBIDDEN_PIPELINE);
        }

        Long inputTopicId = createAISqlConfig.getInputTopicId();

        topicService.getPermissionsOfTopicByUserId(userId)
                .stream()
                .filter(permission -> inputTopicId.equals(permission.getTopicId()))
                .findFirst()
                .orElseThrow(() -> new BaseAPIException(ErrorCode.FORBIDDEN_TOPICS));

        // pipeline AI SQL정보 update
        String naturalLanguageRequest = createAISqlConfig.getNaturalLanguageRequest();
        pipeline.setNaturalLanguageRequest(naturalLanguageRequest);

        // 자연어 요청, Plan JSON update
        PipelinePlan pipelinePlan = createAISqlConfig.getPipelinePlan();

        try {
            String pipelinePlanJson = jsonMapper.writeValueAsString(pipelinePlan);
            pipeline.setPipelinePlanJson(pipelinePlanJson);
        } catch (JsonProcessingException e) {
            log.error("pipeline plan json 문자열로 변경 실패 -> current pipelinePlan: {}", pipelinePlan);
            throw new RuntimeException(e);
        }

        repository.updateAISqlPipeline(pipeline);

        // ai config 설정 insert (topic id)
        AISqlConfig aiSqlConfig = AISqlConfig.builder()
                .pipelineId(pipelineId)
                .inputTopicId(createAISqlConfig.getInputTopicId())
                .build();
        repository.createAISqlConfig(aiSqlConfig);

        return pipelineConverter.toDTO(pipeline);
    }

    @Override
    public PipelineResponse.AISqlPreview aiSqlPipelinePlanPreview(PipelineRequest.AISqlPreview aiSqlPreview) {

        Long topicId = aiSqlPreview.getInputTopicId();

        TopicResponse.Item item = topicService.getTopicById(topicId);
        Topic topic = topicConverter.toVO(item);

        AIDeploymentResponse.GeneratePlan generatePlan =
                aiDeploymentService.getPipelinePlan(topic, null, aiSqlPreview.getNaturalLanguageRequest());
        PipelinePlanValidationContext planValidationContext = generatePlan.getPipelinePlanValidationContext();

        FlinkSQLGenerationContext context =
                pipelineDeploymentConverter.toGenerationContext(planValidationContext);
        String generatedFlinkSql = flinkSQLPreviewGenerator.generate(context);

        return PipelineResponse.AISqlPreview.builder()
                .pipelinePlan(generatePlan.getPipelinePlan())
                .generatedFlinkSql(generatedFlinkSql)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineResponse.Pipeline update(PipelineRequest.Update updateItem) {
        // 사용자 검증
        validateUser(updateItem.getOwnerUserId());

        Pipeline pipeline = pipelineConverter.toVO(updateItem);
        repository.update(pipeline);
        return pipelineConverter.toDTO(pipeline);
    }

    @Override
    public PipelineResponse.Pipeline findPipelineByPipelineId(Long pipelineId) {
        return repository.findPipelineByPipelineId(pipelineId)
                .map(pipelineConverter::toDTO)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));
    }

    @Override
    public PipelineResponse.CustomJarPipeline findCustomJarPipelineByPipelineId(Long pipelineId) {
        Pipeline pipeline = repository.findPipelineByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));
        CustomJobConfig customJobConfig = repository.findCustomJobConfigByPipelineId(pipelineId)
                .orElse(null);
        PipelineArtifact pipelineArtifact = repository.findPipelineArtifactByPipelineId(pipelineId)
                .orElse(null);

        PipelineResponse.CustomJarPipeline result = pipelineConverter.toCustomJarDTO(pipeline);
        result.setCustomJobConfig(pipelineConverter.toDTO(customJobConfig));
        result.setPipelineArtifact(pipelineConverter.toDTO(pipelineArtifact));
        return result;
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
                        throw new BaseAPIException(ErrorCode.CONFLICT_PIPELINE_ARTIFACT);
                    });

        repository.findCustomJobConfigByPipelineId(pipelineId)
                .ifPresent(artifact -> {
                    throw new BaseAPIException(ErrorCode.CONFLICT_CUSTOM_JOB_CONFIG);
                });

        CustomJobConfig customJobConfig = pipelineConverter.toVO(createCustomJobConfig, pipelineId);

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

        return pipelineConverter.toDTO(artifact);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineResponse.PipelineStatus updatePipelineStatus(Long pipelineId) {

        Pipeline pipeline = repository.findPipelineByPipelineId(pipelineId)
            .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));

        PipelineDeployment pipelineDeployment = repository.findLatestPipelineDeployMentByPipelineId(pipelineId)
            .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE_DEPLOYMENT, pipelineId));

        String deployedFlinkJobId = pipelineDeployment.getFlinkJobId();
        if (deployedFlinkJobId == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_FLINK_JOB_ID);
        }

        FlinkJobStatus jobStatus = flinkRestClient.getJobStatus(deployedFlinkJobId);

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
                FlinkResponse.JobException jobExceptions =
                    flinkRestClient.getExceptionsByJobId(deployedFlinkJobId);

                List<JobExceptionsEntry> entries = jobExceptions.getExceptionHistory().getEntries();
                JobExceptionsEntry rootExceptionEntry = entries.get(0);

                pipelineDeployment.setErrorExceptionName(rootExceptionEntry.getExceptionName());
                pipelineDeployment.setErrorMessage(rootExceptionEntry.getStacktrace());

                LocalDateTime timestamp =
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(rootExceptionEntry.getTimestamp()), ZoneId.systemDefault());
                pipelineDeployment.setErrorTimestamp(timestamp);

                repository.updatePipelineDeploymentError(pipelineDeployment);
            } catch (Exception e) {
//                pipeline.setPipelineStatus(PipelineStatus.FAILED);
//                pipelineDeployment.setStatus(DeploymentStatus.FAILED);
//                log.error(e.getMessage());
                throw new BaseAPIException(ErrorCode.FAILED_PIPELINE_STATUS_SYNC);
            }
        }

        repository.updatePipelineStatus(pipeline);
        repository.updatePipelineDeploymentStatus(pipelineDeployment);

        return PipelineResponse.PipelineStatus
                .builder()
                .pipelineId(pipeline.getPipelineId())
                .deploymentId(pipelineDeployment.getDeploymentId())
                .flinkJobId(deployedFlinkJobId)
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

    @Override
    public PipelineResponse.PipelineStatus findPipelineFailuresByPipelineId(Long pipelineId) {

        Pipeline pipeline = repository.findPipelineByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));

        PipelineDeployment deployment = repository.findLatestPipelineDeployMentByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE_DEPLOYMENT));

        return PipelineResponse.PipelineStatus.builder()
                .pipelineId(pipelineId)
                .deploymentId(deployment.getDeploymentId())
                .flinkJobId(deployment.getFlinkJobId())
                .pipelineStatus(pipeline.getPipelineStatus())
                .deploymentStatus(deployment.getStatus())
                .failure(PipelineResponse.PipelineStatus.Failure.from(
                        deployment.getErrorExceptionName(),
                        deployment.getErrorMessage(),
                        deployment.getErrorTimestamp()
                )).build();
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
