package com.streamcell.platform.ai.service;

import com.streamcell.platform.ai.domain.context.PipelinePlanValidationContext;
import com.streamcell.platform.ai.dto.AIDeploymentResponse;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.topic.vo.Topic;

public interface AIDeploymentService {

    AIDeploymentResponse.GeneratePlan getPipelinePlan(Topic topic, Long pipelineId, String sendMessage);

    PipelinePlanValidationContext validatePipelinePlan(Long pipelineId, PipelinePlan pipelinePlan);

}
