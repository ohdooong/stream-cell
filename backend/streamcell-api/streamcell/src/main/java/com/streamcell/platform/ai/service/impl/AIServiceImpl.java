package com.streamcell.platform.ai.service.impl;

import com.streamcell.platform.ai.converter.AIConverter;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContextResolver;
import com.streamcell.platform.ai.domain.validator.*;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final PipelinePlanValidationContextResolver pipelinePlanValidationContextResolver;

    @Override
    public void requestPipelinePlan() {

        validateForPipelinePlan(new PipelinePlan());


    }

    private void validateForPipelinePlan(PipelinePlan pipelinePlan) {
        PipelinePlanValidationContext context =
                pipelinePlanValidationContextResolver.resolve(1L, 1L, pipelinePlan);

        CompositeValidator<PipelinePlanValidationContext> compositeValidator =
                new CompositeValidator<PipelinePlanValidationContext>()
                        .add(new BasicValidator())
                        .add(new PipelineValidator())
                        .add(new TopicValidator())
                        .add(new TopicPermissionValidator())
                        .add(new WindowValidator())
                        .add(new SchemaValidator())
                        .add(new AggregationValidator())
                        .add(new FilterValidator());

        compositeValidator.validate(context);
    }
}
