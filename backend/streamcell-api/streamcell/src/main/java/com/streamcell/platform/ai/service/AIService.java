package com.streamcell.platform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.streamcell.platform.ai.dto.FlinkSQLGatewayResponse;

public interface AIService {

    /**
     *
     */
    void requestPipelinePlan();


    FlinkSQLGatewayResponse.SubmitSQL flinkSqlGatewayTest() throws JsonProcessingException;


}
