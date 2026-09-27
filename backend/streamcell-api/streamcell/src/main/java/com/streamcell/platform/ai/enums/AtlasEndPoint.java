package com.streamcell.platform.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
public enum AtlasEndPoint {
    CREATE_SESSION("", "세션생성");

    private final String path;
    private String description;
}
