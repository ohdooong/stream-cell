package com.streamcell.platform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.streamcell.platform.ai.dto.AIDeploymentResponse;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.dto.PipelineResponse;

public interface AIDeploymentService {

    AIDeploymentResponse.GeneratePlan getPipelinePlanByPipelineId(Long pipelineId);

}
