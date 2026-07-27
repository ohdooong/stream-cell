package com.streamcell.platform.pipeline.vo;

import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pipeline {
    private Long pipelineId;
    private Long ownerUserId;
    private String pipelineName;
    private String description;
    private PipelineType pipelineType;
    private PipelineStatus pipelineStatus;
    private String naturalLanguageRequest;
    private String pipelinePlanJson;
    private String generatedSql;
}
