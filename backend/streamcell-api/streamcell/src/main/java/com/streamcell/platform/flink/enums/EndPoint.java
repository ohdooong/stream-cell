package com.streamcell.platform.flink.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EndPoint {
    OVERVIEW("/overview"),
    UPLOAD_JAR("/jars/upload"),
    RUN_JAR("/jars/%s/run", "Flink Jar을 Run /jars/{flinkJarId}/run"),
    JOB_STATUS("/jobs/%s/status", "Flink Job status조회 /jobs/{flinkJobId}/status");

    EndPoint(String path) {
        this.path = path;
    }

    private final String path;
    private String description;

}
