package com.streamcell.platform.pipeline.dto;

import com.streamcell.platform.pipeline.enums.PipelineStatus;
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
        private Long pipelineId;

        @NotNull
        private Long ownerUserId;

        @NotBlank
        private String pipelineName;

        @NotNull
        private PipelineType pipelineType;

        @NotNull
        private PipelineStatus pipelineStatus;

        private String naturalLanguageRequest;
        private String pipelinePlanJson;
        private String generatedSql;
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

        @NotNull
        private PipelineType pipelineType;

        @NotNull
        private PipelineStatus pipelineStatus;
        private String naturalLanguageRequest;
        private String pipelinePlanJson;
        private String generatedSql;
    }
}

