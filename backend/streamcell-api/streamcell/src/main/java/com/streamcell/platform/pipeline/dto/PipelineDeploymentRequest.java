package com.streamcell.platform.pipeline.dto;

import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PipelineDeploymentRequest {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class Create {
        private Long pipelineId;
        private PipelineType deploymentType;
        private String flinkJobId;
        private String flinkJarId;
        private DeploymentStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime stoppedAt;
        private LocalDateTime finishedAt;
        private LocalDateTime lastCheckedAt;
        private String errorExceptionName;
        private String errorMessage;
        private Long errorTimestamp;
    }

}
