package com.streamcell.platform.pipeline.service;

import com.streamcell.platform.pipeline.dto.PipelineResultQueryResponse;

public interface PipelineResultQueryService {

    PipelineResultQueryResponse.Dashboard getPipelineResults(Long pipelineId, Integer limit, Integer rangeMinutes);

}
