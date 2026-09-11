package com.streamcell.platform.ai.service.impl;

import com.streamcell.platform.ai.converter.AIConverter;
import com.streamcell.platform.ai.domain.context.FlinkSQLGenerationContext;
import com.streamcell.platform.ai.domain.context.KafkaSourceDDLGenerationContext;
import com.streamcell.platform.ai.domain.context.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.resolver.PipelinePlanValidationContextResolver;
import com.streamcell.platform.ai.domain.generator.FlinkSQLGenerator;
import com.streamcell.platform.ai.domain.generator.KafkaSourceDDLGenerator;
import com.streamcell.platform.ai.domain.validator.AggregationValidator;
import com.streamcell.platform.ai.domain.validator.BasicValidator;
import com.streamcell.platform.ai.domain.validator.CompositeValidator;
import com.streamcell.platform.ai.domain.validator.FilterValidator;
import com.streamcell.platform.ai.domain.validator.PipelineValidator;
import com.streamcell.platform.ai.domain.validator.SchemaValidator;
import com.streamcell.platform.ai.domain.validator.TopicPermissionValidator;
import com.streamcell.platform.ai.domain.validator.TopicValidator;
import com.streamcell.platform.ai.domain.validator.WindowValidator;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AIConverter aiConverter;
    private final PipelinePlanValidationContextResolver pipelinePlanValidationContextResolver;
    private final FlinkSQLGenerator flinkSQLGenerator;
    private final KafkaSourceDDLGenerator kafkaSourceDDLGenerator;


    @Override
    public void requestPipelinePlan() {
        PipelinePlanValidationContext validationContext =
            validateForPipelinePlan(new PipelinePlan());

        FlinkSQLGenerationContext generationContext =
            aiConverter.toGenerationContext(validationContext);

        String generate = flinkSQLGenerator.generate(generationContext);

        KafkaSourceDDLGenerationContext kafkaSourceDDLGenerationContext
                = aiConverter.toKafkaSourceDDLGenerationContext(validationContext);

        String generate1 = kafkaSourceDDLGenerator.generate(kafkaSourceDDLGenerationContext);

    }

    private PipelinePlanValidationContext validateForPipelinePlan(PipelinePlan pipelinePlan) {
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
        return context;
    }
}
