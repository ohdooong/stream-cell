package com.streamcell.platform.ai.domain.context;

import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.topic.vo.Topic;
import com.streamcell.platform.topic.vo.TopicPermission;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class FlinkSQLGenerationContext {
    private Long userId;
    private PipelinePlan pipelinePlan;
    private Pipeline pipeline;
    private Topic sourceTopic;
}
