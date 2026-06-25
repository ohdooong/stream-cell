package com.streamcell.platform.topic.vo;

import com.streamcell.platform._common.enums.TopicPermissionType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TopicPermission {
    private Long permissionId;
    private Long topicId;
    private String topicName;
    private Long userId;
    private String userName;
    private TopicPermissionType topicPermissionType;
}
