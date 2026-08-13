package com.streamcell.platform.pipeline.domain;

import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeploymentStatusPolicy {

    private final List<DeploymentStatus> availableCancelDeploymentStatus = List.of(
            //DeploymentStatus.DEPLOYING,
            DeploymentStatus.RUNNING
    );

    public boolean isAvailableStop(DeploymentStatus deploymentStatus) {
        return availableCancelDeploymentStatus.contains(deploymentStatus);
    }
}
