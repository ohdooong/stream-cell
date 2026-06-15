package com.streamcell.platform.topic.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Topic {
    private Long topicId;
    private String topicName;
    private String displayName;
    private String description;
    private String schemaJson;
    private String timeField;
    private String messageFormat;
}
