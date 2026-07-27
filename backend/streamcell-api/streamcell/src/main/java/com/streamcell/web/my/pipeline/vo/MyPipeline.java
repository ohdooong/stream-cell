package com.streamcell.web.my.pipeline.vo;

import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MyPipeline {
    private Long pipelineId;
    private Long ownerUserId;
    private String pipelineName;
    private PipelineType pipelineType;
    private PipelineStatus pipelineStatus;
    private String naturalLanguageRequest;
    private String pipelinePlanJson;
    private String generatedSql;
}
