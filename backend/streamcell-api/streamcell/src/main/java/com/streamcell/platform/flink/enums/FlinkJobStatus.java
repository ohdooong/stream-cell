package com.streamcell.platform.flink.enums;


public enum FlinkJobStatus {
    INITIALIZING,
    CREATED,
    RUNNING,
    FAILING,
    FAILED,
    CANCELLING,
    CANCELED,
    FINISHED,
    RESTARTING,
    SUSPENDED,
    RECONCILING;

    public static FlinkJobStatus from(String flinkJobStatus) {
        if (flinkJobStatus == null || flinkJobStatus.isBlank()) {
            return null;
        }

        for (FlinkJobStatus status : FlinkJobStatus.values()) {
            if (flinkJobStatus.equals(status.name())) {
                return status;
            }
        }

        return null;
    }
}
