package com.streamcell.platform.topic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


public class TopicResponse {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Items {
        private Long topicId;
        private String topicName;
        private String displayName;
        private String description;
        private String schemaJson;
        private String timeField;
        private String messageFormat;
    }
}
