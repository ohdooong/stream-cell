package com.streamcell.platform.pipeline.service.impl;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.flink.client.FlinkJarClient;
import com.streamcell.platform.flink.dto.FlinkResponse;
import com.streamcell.platform.flink.util.FlinkUtils;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.service.PipelineDeploymentService;
import com.streamcell.platform.pipeline.validator.PipelineValidator;
import com.streamcell.platform.pipeline.vo.CustomJobConfig;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import com.streamcell.platform.pipeline.vo.PipelineDeployment;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PipelineDeploymentServiceImpl implements PipelineDeploymentService {

    private final PipelineRepository repository;
    private final FlinkJarClient flinkJarClient;

    private final Map<String, PipelineValidator<?>> validatorMap;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deploy(Long pipelineId) {

        Pipeline pipeline =
                repository.findPipelineByPipelineId(pipelineId)
                        .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));
        PipelineArtifact artifact =
                repository.findPipelineArtifactByPipelineId(pipelineId)
                        .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE_ARTIFACT));

        PipelineValidator<Pipeline, PipelineArtifact> pipelineDeploymentValidator = getPipelineDeploymentValidator();
        pipelineDeploymentValidator.validate(pipeline, artifact);

        // 실제 jar 업로드
        FlinkResponse.JarUploadResponse jarUploadResponse = flinkJarClient.uploadJar(artifact);
        if (!"success".equals(jarUploadResponse.getStatus())) {
            throw new BaseAPIException(ErrorCode.FAILED_UPLOAD_JAR);
        }

        // flinkJarId update
        String flinkJarId = FlinkUtils.extractFlinkJarId(jarUploadResponse.getFilename());
        artifact.setFlinkJarId(flinkJarId);
        repository.updatePipelineArtifactForFlinkJarId(artifact);

        CustomJobConfig customJobConfig = repository.findCustomJobConfigByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE_ARTIFACT));

        FlinkResponse.JarRunResponse runJarResponse = flinkJarClient.runJar(pipeline, artifact, customJobConfig);

        PipelineDeployment pipelineDeployment = PipelineDeployment
                .builder()
                .pipelineId(pipelineId)
                .deploymentType(PipelineType.AI_SQL)
                .flinkJobId(runJarResponse.getJobId())
                .flinkJarId(artifact.getFlinkJarId())
                .status(DeploymentStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();

        repository.insertPipelineDeployment(pipelineDeployment);


    }

    private PipelineValidator<Pipeline, PipelineArtifact> getPipelineDeploymentValidator() {
        PipelineValidator<Pipeline, PipelineArtifact> pipelineDeploymentValidator =
                (PipelineValidator<Pipeline, PipelineArtifact>) validatorMap.get("pipelineDeploymentValidator");
        if (pipelineDeploymentValidator == null) {
            throw new RuntimeException("validator Bean을 찾을 수 없습니다.");
        }
        return pipelineDeploymentValidator;
    }


//    private String getFlinkJobId(FlinkResponse.JarUploadResponse jarUploadResponse) {
//        JsonMapper mapper = new JsonMapper();
//        JsonNode jsonNode = mapper.readTree(jarUploadResponse.getProperties().toString());
//        return jsonNode.path("filename").path("type").asString();
//    }

}
