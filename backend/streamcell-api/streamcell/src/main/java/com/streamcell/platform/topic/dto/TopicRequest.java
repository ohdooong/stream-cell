package com.streamcell.platform.topic.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.streamcell.platform._common.enums.TopicPermissionType;
import com.streamcell.platform.topic.enums.MessageFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class TopicRequest {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class Schema {
        private String displayName;
        private String description;
        private MessageFormat messageFormat;
        private String timeField;

        @NotBlank
        @JsonRawValue
        private String schemaJson;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class TopicPermission {
        List<Long> userIds;
        TopicPermissionType topicPermissionType;
    }
}
