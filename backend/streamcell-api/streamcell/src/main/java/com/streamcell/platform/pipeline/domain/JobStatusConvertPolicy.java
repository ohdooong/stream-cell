package com.streamcell.platform.pipeline.domain;

import com.streamcell.platform.flink.enums.FlinkJobStatus;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobStatusConvertPolicy {

    private final List<FlinkJobStatus> deploying = List.of(
            FlinkJobStatus.INITIALIZING,
            FlinkJobStatus.CREATED,
            FlinkJobStatus.RECONCILING,
            FlinkJobStatus.RESTARTING
    );

    private final List<FlinkJobStatus> failed = List.of(FlinkJobStatus.FAILING, FlinkJobStatus.FAILED);

    public PipelineStatus convertToPipelineStatusFrom(FlinkJobStatus flinkJobStatus) {

        if (deploying.contains(flinkJobStatus)) {
            return PipelineStatus.DEPLOYING;
        }

        if (failed.contains(flinkJobStatus)) {
            return PipelineStatus.FAILED;
        }

        if (FlinkJobStatus.RUNNING == flinkJobStatus) {
            return PipelineStatus.RUNNING;
        }

        if (FlinkJobStatus.CANCELLING == flinkJobStatus) {
            return PipelineStatus.STOPPING;
        }

        if (FlinkJobStatus.CANCELED == flinkJobStatus) {
            return PipelineStatus.STOPPED;
        }

        if (FlinkJobStatus.FINISHED == flinkJobStatus) {
            return PipelineStatus.FINISHED;
        }

        if (FlinkJobStatus.SUSPENDED == flinkJobStatus) {
            return PipelineStatus.SUSPENDED;
        }

        return null;
    }

    public DeploymentStatus convertToDeploymentStatusFrom(FlinkJobStatus flinkJobStatus) {
        if (deploying.contains(flinkJobStatus)) {
            return DeploymentStatus.DEPLOYING;
        }

        if (failed.contains(flinkJobStatus)) {
            return DeploymentStatus.FAILED;
        }

        if (FlinkJobStatus.RUNNING == flinkJobStatus) {
            return DeploymentStatus.RUNNING;
        }

        if (FlinkJobStatus.CANCELLING == flinkJobStatus) {
            return DeploymentStatus.STOPPING;
        }

        if (FlinkJobStatus.CANCELED == flinkJobStatus) {
            return DeploymentStatus.STOPPED;
        }

        if (FlinkJobStatus.FINISHED == flinkJobStatus) {
            return DeploymentStatus.FINISHED;
        }

        if (FlinkJobStatus.SUSPENDED == flinkJobStatus) {
            return DeploymentStatus.SUSPENDED;
        }

        return null;
    }
}
