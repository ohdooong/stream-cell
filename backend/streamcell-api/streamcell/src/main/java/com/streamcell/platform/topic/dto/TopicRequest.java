package com.streamcell.platform.topic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class TopicRequest {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class Schema {
        private String displayName;
        private String description;
        private String messageFormat;
        private String timeField;
        private String schemaJson;
    }
}
