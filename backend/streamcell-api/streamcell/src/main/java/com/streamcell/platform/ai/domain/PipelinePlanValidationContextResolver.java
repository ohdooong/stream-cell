package com.streamcell.platform.ai.domain;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.topic.repository.TopicRepository;
import com.streamcell.platform.topic.vo.Topic;
import com.streamcell.platform.topic.vo.TopicPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 검증할데이터를 준비하는 Resolver
 */
@Component
@RequiredArgsConstructor
public class PipelinePlanValidationContextResolver {
    private final PipelineRepository pipelineRepository;
    private final TopicRepository topicRepository;

    public PipelinePlanValidationContext resolve(
            Long userId,
            Long pipelineId,
            PipelinePlan pipelinePlan
    ) {
        Pipeline pipeline = pipelineRepository.findPipelineByPipelineId(pipelineId)
                .orElse(null);

        Long sourceTopicId = pipelinePlan.getSourceTopicId();
        Topic topic = topicRepository.findById(sourceTopicId)
                .orElse(null);

        List<TopicPermission> permissions = topicRepository.findTopicPermissionByUserId(userId);

        return PipelinePlanValidationContext.builder()
                .userId(userId)
                .pipeline(pipeline)
                .sourceTopic(topic)
                .pipelinePlan(pipelinePlan)
                .topicPermissions(permissions)
                .build();
    }
}

