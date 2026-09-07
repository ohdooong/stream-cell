package com.streamcell.platform.ai.domain.context;

import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.topic.vo.Topic;
import com.streamcell.platform.topic.vo.TopicPermission;
import lombok.*;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class KafkaSourceDDLGenerationContext {
    private Long userId;
    private PipelinePlan pipelinePlan;
    private Pipeline pipeline;

    private Topic sourceTopic;
    private Map<String, Object> parsedTopicSchema;
}
