package com.streamcell.web.my.pipeline.dto;

import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import lombok.*;

public class MyPipelineResponse {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class Pipeline {
        private Long pipelineId;
        private Long ownerUserId;
        private String pipelineName;
        private PipelineType pipelineType;
        private PipelineStatus pipelineStatus;
        private String naturalLanguageRequest;
        private String pipelinePlanJson;
        private String generatedSql;
    }
}
