package com.streamcell.platform.ai.domain.validator;


import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.topic.vo.Topic;

public class SchemaValidator implements Validator<PipelinePlanValidationContext> {

    @Override
    public void validate(PipelinePlanValidationContext context) {
        PipelinePlan pipelinePlan = context.getPipelinePlan();
        Topic topic = context.getSourceTopic();

        // 2026-09-04 todo 구현진행
    }
}
