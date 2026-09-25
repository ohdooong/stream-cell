package com.streamcell.platform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.streamcell.platform.pipeline.dto.PipelineResponse;

public interface AIDeploymentService {

    PipelineResponse.Deployment flinkSqlGatewayTest() throws JsonProcessingException;


}
