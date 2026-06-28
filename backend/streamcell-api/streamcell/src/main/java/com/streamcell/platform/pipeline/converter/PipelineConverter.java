package com.streamcell.platform.pipeline.converter;

import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.vo.Pipeline;


public class PipelineConverter {

    public static PipelineResponse.Pipeline toDTO(Pipeline vo) {
        return PipelineResponse.Pipeline.builder()
                .pipelineId(vo.getPipelineId())
                .ownerUserId(vo.getOwnerUserId())
                .pipelineName(vo.getPipelineName())
                .description(vo.getDescription())
                .pipelineType(vo.getPipelineType())
                .pipelineStatus(vo.getPipelineStatus())
                .naturalLanguageRequest(vo.getNaturalLanguageRequest())
                .pipelinePlanJson(vo.getPipelinePlanJson())
                .generatedSql(vo.getGeneratedSql())
                .build();
    }

    public static Pipeline toVO(PipelineRequest.Create createItem) {
        return Pipeline.builder()
                .ownerUserId(createItem.getOwnerUserId())
                .pipelineName(createItem.getPipelineName())
                .description(createItem.getDescription())
                .pipelineType(createItem.getPipelineType())
                .pipelineStatus(PipelineStatus.CREATED)
                .build();
    }

    public static Pipeline toVO(PipelineRequest.Update updateItem) {
        return Pipeline.builder()
                .pipelineId(updateItem.getPipelineId())
                .ownerUserId(updateItem.getOwnerUserId())
                .pipelineName(updateItem.getPipelineName())
                .description(updateItem.getDescription())
                .pipelineType(updateItem.getPipelineType())
                .build();
    }

}
