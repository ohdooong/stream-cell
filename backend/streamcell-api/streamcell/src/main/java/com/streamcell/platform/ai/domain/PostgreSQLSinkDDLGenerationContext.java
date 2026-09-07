package com.streamcell.platform.ai.domain;

import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.topic.vo.Topic;
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
public class PostgreSQLSinkDDLGenerationContext {
    private Long userId;
    private PipelinePlan pipelinePlan;
    private Pipeline pipeline;
    private Topic sourceTopic;
}
