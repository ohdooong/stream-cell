package com.streamcell.platform.pipeline.dto;

import com.streamcell.platform.pipeline.enums.PipelineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

public class PipelineRequest {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class Create {

        @NotNull
        private Long ownerUserId;

        @NotBlank
        private String pipelineName;

        private String description;

        @NotNull
        private PipelineType pipelineType;


    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class Update {
        @NotNull
        private Long pipelineId;

        @NotNull
        private Long ownerUserId;

        @NotBlank
        private String pipelineName;

        private String description;

        @NotNull
        private PipelineType pipelineType;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class CreateCustomJobConfig {
        private Long userId;
        private String entryClass;
        private List<Long> inputTopicIds;
        private List<Long> outputTopicIds;
        private Integer parallelism;
        private Map<String, Object> programArgs;
    }
}

