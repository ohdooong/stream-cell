package com.streamcell.platform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.streamcell.platform.flink.dto.FlinkSQLGatewayResponse;
import com.streamcell.platform.pipeline.dto.PipelineResponse;

public interface AIService {

    /**
     *
     */
    void requestPipelinePlan();


    PipelineResponse.Deployment flinkSqlGatewayTest() throws JsonProcessingException;


}
