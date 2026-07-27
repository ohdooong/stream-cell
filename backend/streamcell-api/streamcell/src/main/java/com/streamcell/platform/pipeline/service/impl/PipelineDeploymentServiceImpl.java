package com.streamcell.platform.pipeline.service.impl;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.flink.client.FlinkJarClient;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.service.PipelineDeploymentService;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class PipelineDeploymentServiceImpl implements PipelineDeploymentService {

    private final PipelineRepository repository;
    private final FlinkJarClient flinkJarClient;

    @Override
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


    }

    private boolean isExistJar(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

}
