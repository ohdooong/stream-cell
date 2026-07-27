package com.streamcell.platform.pipeline.service.impl;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.flink.client.FlinkJarClient;
import com.streamcell.platform.flink.dto.FlinkResponse;
import com.streamcell.platform.flink.util.FlinkUtils;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.service.PipelineDeploymentService;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PipelineDeploymentServiceImpl implements PipelineDeploymentService {

    private final PipelineRepository repository;
    private final FlinkJarClient flinkJarClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deploy(Long pipelinId) {
        Pipeline pipeline = repository.findPipelineByPipelineId(pipelinId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));

        if (PipelineType.CUSTOM_JAR != pipeline.getPipelineType()) {
            throw new BaseAPIException(ErrorCode.BAD_REQUEST_NOT_CUSTOM_JAR_TYPE);
        }

        if (PipelineStatus.ARTIFACT_UPLOADED != pipeline.getPipelineStatus()) {
            throw new BaseAPIException(ErrorCode.BAD_REQUEST_NOT_UPLOADED_CUSTOM_JAR);
        }

        PipelineArtifact artifact = repository.findPipelineArtifactByPipelineId(pipelinId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE_ARTIFACT));

        String storedFilePath = artifact.getStoredFilePath();
        if (!isExistJar(storedFilePath)) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_FILE);
        }

        if (artifact.getFlinkJarId() != null) {
            throw new BaseAPIException(ErrorCode.CONFLICT_FLINK_JAR_ID);
        }

        // 실제 jar 업로드
        FlinkResponse.JarUploadResponse jarUploadResponse = flinkJarClient.uploadJar(artifact);
        if (!"success".equals(jarUploadResponse.getStatus())) {
            throw new BaseAPIException(ErrorCode.FAILED_UPLOAD_JAR);
        }
//        String flinkJobId = getFlinkJobId(jarUploadResponse);
        String flinkJarId = FlinkUtils.extractFlinkJarId(jarUploadResponse.getFilename());
        artifact.setFlinkJarId(flinkJarId);

        repository.updatePipelineArtifactForFlinkJarId(artifact);
        // flink jar id 업데이트
    }

    private boolean isExistJar(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

//    private String getFlinkJobId(FlinkResponse.JarUploadResponse jarUploadResponse) {
//        JsonMapper mapper = new JsonMapper();
//        JsonNode jsonNode = mapper.readTree(jarUploadResponse.getProperties().toString());
//        return jsonNode.path("filename").path("type").asString();
//    }

}
