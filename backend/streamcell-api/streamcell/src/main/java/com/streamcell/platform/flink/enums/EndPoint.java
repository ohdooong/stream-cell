package com.streamcell.platform.flink.enums;

import lombok.Getter;

@Getter
public enum EndPoint {
    OVERVIEW("/overview"),
    UPLOAD_JAR("/jars/upload");

    EndPoint(String path) {
        this.path = path;
    }
    private final String path;

}
