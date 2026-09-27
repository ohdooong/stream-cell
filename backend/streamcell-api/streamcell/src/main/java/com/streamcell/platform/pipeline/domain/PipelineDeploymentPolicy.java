package com.streamcell.platform.pipeline.domain;

import com.streamcell.platform.pipeline.enums.PipelineStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PipelineDeploymentPolicy {

    private final List<PipelineStatus> availableDeployPipelineStatus =
            List.of(
                    PipelineStatus.DRAFT,
                    PipelineStatus.CREATED
            );

    public boolean isDeployPipeline(PipelineStatus pipelineStatus) {
        return availableDeployPipelineStatus.contains(pipelineStatus);
    }
}
