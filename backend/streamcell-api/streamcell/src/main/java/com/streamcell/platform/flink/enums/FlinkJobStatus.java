package com.streamcell.platform.flink.enums;


import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;

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

        throw new BaseAPIException(ErrorCode.INVALID_FLINK_JOB_STATUS);
    }
}
