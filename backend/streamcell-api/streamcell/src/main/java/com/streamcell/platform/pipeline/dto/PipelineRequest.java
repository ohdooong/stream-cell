package com.streamcell.platform.pipeline.dto;

import com.streamcell.platform.pipeline.enums.PipelineType;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @AllArgsConstructor(staticName = "from")
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
    @AllArgsConstructor(staticName = "from")
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
    @AllArgsConstructor(staticName = "from")
    public static class CreateCustomJobConfig {
        @Schema(description = "userId", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private Long userId;

        @NotBlank
        @Schema(description = "entryClass", example = "com.example.Myjob", requiredMode = Schema.RequiredMode.REQUIRED)
        private String entryClass;

        @Schema(description = "inputTopicIds", example = "[\"1\", \"2\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private List<Long> inputTopicIds;

        private List<Long> outputTopicIds;

        @Schema(description = "inputTopicIds", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private Integer parallelism;

        @Schema(description = "programArgs", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private Map<String, String> programArgs;

//        @NotNull
//        private Map<String, Object> programArgs;
    }
}

