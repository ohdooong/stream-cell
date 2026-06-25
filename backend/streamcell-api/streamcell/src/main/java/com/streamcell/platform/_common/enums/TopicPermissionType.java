package com.streamcell.platform._common.enums;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public enum TopicPermissionType {
    VIEW("Topic 조회 가능"),
    QUERY("AI SQL에서 사용 가능"),
    DEPLOY("해당 Topic 기반 Job 배포 가능"),
    ADMIN("Schema/권한 수정 가능");

    private String description;

    TopicPermissionType(String description) {
        this.description = description;
    }
}
