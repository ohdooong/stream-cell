package com.streamcell.web.my.pipeline.converter;

import com.streamcell.web.my.pipeline.dto.MyPipelineResponse;
import com.streamcell.web.my.pipeline.vo.MyPipeline;

public class MyPipelineConverter {
    public static MyPipelineResponse.Pipeline toDTO(MyPipeline vo) {
        return MyPipelineResponse.Pipeline.builder()
                .pipelineId(vo.getPipelineId())
                .ownerUserId(vo.getOwnerUserId())
                .pipelineName(vo.getPipelineName())
                .pipelineType(vo.getPipelineType())
                .pipelineStatus(vo.getPipelineStatus())
                .naturalLanguageRequest(vo.getNaturalLanguageRequest())
                .pipelinePlanJson(vo.getPipelinePlanJson())
                .generatedSql(vo.getGeneratedSql())
                .build();
    }
}
