package com.streamcell.platform.ai.domain;

import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.topic.vo.Topic;
import com.streamcell.platform.topic.vo.TopicPermission;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PipelinePlanValidationContext {
    private Long userId;
    private PipelinePlan pipelinePlan;
    private Pipeline pipeline;
    private Topic sourceTopic;
    private List<TopicPermission> topicPermissions;
}