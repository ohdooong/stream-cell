package com.streamcell.platform.topic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


public class TopicResponse {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class Item {

        private Long topicId;
        private String topicName;
        private String displayName;
        private String description;
        private String schemaJson;
        private String timeField;
        private String messageFormat;
    }
}
