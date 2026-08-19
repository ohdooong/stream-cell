package com.streamcell.platform.pipeline.validator;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;

@Component("pipelineDeploymentValidator")
@RequiredArgsConstructor
public class PipelineDeploymentValidator implements PipelineValidator<Pipeline, PipelineArtifact> {

    private final PipelineRepository repository;

    @Override
    public void validate(Pipeline pipeline, PipelineArtifact artifact) {

        if (PipelineType.CUSTOM_JAR != pipeline.getPipelineType()) {
            throw new BaseAPIException(ErrorCode.BAD_REQUEST_NOT_CUSTOM_JAR_TYPE);
        }

        PipelineStatus pipelineStatus = pipeline.getPipelineStatus();

        if (pipelineStatus == null
        || PipelineStatus.DRAFT == pipelineStatus
        || PipelineStatus.CREATED == pipelineStatus) {
            throw new BaseAPIException(ErrorCode.BAD_REQUEST_NOT_UPLOADED_CUSTOM_JAR);
        }

        if (PipelineStatus.DEPLOYING == pipelineStatus
                || PipelineStatus.RUNNING == pipelineStatus) {
            throw new BaseAPIException(ErrorCode.CONFLICT_PIPELINE_DEPLOYMENT);
        }

        String storedFilePath = artifact.getStoredFilePath();
        if (!isExistJar(storedFilePath)) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_FILE);
        }

        if (artifact.getFlinkJarId() != null
         && PipelineStatus.FAILED != pipelineStatus) { // 실패상태면 재시도 가능하게
            throw new BaseAPIException(ErrorCode.CONFLICT_FLINK_JAR_ID);
        }
    }

    private boolean isExistJar(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }
}
