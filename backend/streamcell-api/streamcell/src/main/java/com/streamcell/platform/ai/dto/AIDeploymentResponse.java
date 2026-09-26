package com.streamcell.platform.ai.dto;

import com.streamcell.platform.ai.domain.context.PipelinePlanValidationContext;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import lombok.*;

public class AIDeploymentResponse {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class GeneratePlan {
        private PipelinePlan pipelinePlan;
        private PipelinePlanValidationContext pipelinePlanValidationContext;
    }

}
