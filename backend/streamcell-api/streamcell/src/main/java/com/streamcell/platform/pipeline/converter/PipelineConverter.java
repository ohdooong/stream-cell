package com.streamcell.platform.pipeline.converter;

import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.vo.Pipeline;


public class PipelineConverter {

    public static PipelineResponse.Pipeline toDTO(Pipeline vo) {
        return PipelineResponse.Pipeline.builder()
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

    public static Pipeline toVO(PipelineRequest.Create createItem) {
        return Pipeline.builder()
                .pipelineId(createItem.getPipelineId())
                .ownerUserId(createItem.getOwnerUserId())
                .pipelineName(createItem.getPipelineName())
                .pipelineType(createItem.getPipelineType())
                .pipelineStatus(createItem.getPipelineStatus())
                .naturalLanguageRequest(createItem.getNaturalLanguageRequest())
                .pipelinePlanJson(createItem.getPipelinePlanJson())
                .generatedSql(createItem.getGeneratedSql())
                .build();
    }

    public static Pipeline toVO(PipelineRequest.Update updateItem) {
        return Pipeline.builder()
                .pipelineId(updateItem.getPipelineId())
                .ownerUserId(updateItem.getOwnerUserId())
                .pipelineName(updateItem.getPipelineName())
                .pipelineType(updateItem.getPipelineType())
                .pipelineStatus(updateItem.getPipelineStatus())
                .naturalLanguageRequest(updateItem.getNaturalLanguageRequest())
                .pipelinePlanJson(updateItem.getPipelinePlanJson())
                .generatedSql(updateItem.getGeneratedSql())
                .build();
    }

}
