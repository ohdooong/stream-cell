package com.streamcell.platform.ai.service;

import com.streamcell.platform.ai.dto.AIDeploymentResponse;
import com.streamcell.platform.topic.vo.Topic;

public interface AIDeploymentService {

    AIDeploymentResponse.GeneratePlan getPipelinePlan(Topic topic, Long pipelineId, String sendMessage);

}
