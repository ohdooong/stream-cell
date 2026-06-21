package com.streamcell.platform.topic.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Topic {
    private Long topicId;
    private String topicName;
    private String displayName;
    private String description;
    private String schemaJson;
    private String timeField;
    private String messageFormat;
}
