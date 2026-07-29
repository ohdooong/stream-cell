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

        if (PipelineStatus.ARTIFACT_UPLOADED != pipeline.getPipelineStatus()) {
            throw new BaseAPIException(ErrorCode.BAD_REQUEST_NOT_UPLOADED_CUSTOM_JAR);
        }

        String storedFilePath = artifact.getStoredFilePath();
        if (!isExistJar(storedFilePath)) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_FILE);
        }

        if (artifact.getFlinkJarId() != null) {
            throw new BaseAPIException(ErrorCode.CONFLICT_FLINK_JAR_ID);
        }
    }

    private boolean isExistJar(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }
}
