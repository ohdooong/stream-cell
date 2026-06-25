package com.streamcell.platform.pipeline.service;

import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;

public interface PipelineService {

    PipelineResponse.Pipeline create(PipelineRequest.Create createItem);

    PipelineResponse.Pipeline update(PipelineRequest.Update updateItem);

    PipelineResponse.Pipeline findPipelineByPipelineId(Long pipelineId);

}
