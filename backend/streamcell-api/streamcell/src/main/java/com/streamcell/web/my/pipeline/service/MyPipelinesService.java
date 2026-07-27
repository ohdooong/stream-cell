package com.streamcell.web.my.pipeline.service;

import com.streamcell.web.my.pipeline.dto.MyPipelineResponse;

import java.util.List;

public interface MyPipelinesService {

    List<MyPipelineResponse.Pipeline> findPipelinesByUserId(Long userId);
}
