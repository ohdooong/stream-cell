package com.streamcell.platform.pipeline.vo;

import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineDeployment {
    private Long deploymentId;
    private Long pipelineId;
    private PipelineType deploymentType;
    private String flinkJobId;
    private String flinkJarId;
    private DeploymentStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime lastCheckedAt;
    private String errorMessage;
}
