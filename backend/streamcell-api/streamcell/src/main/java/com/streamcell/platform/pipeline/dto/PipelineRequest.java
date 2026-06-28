package com.streamcell.platform.pipeline.dto;

import com.streamcell.platform.pipeline.enums.PipelineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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
    public static class CreateCustomJar {

    }

}

