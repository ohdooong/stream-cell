package com.streamcell.platform.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.platform.ai.client.FlinkSQLGatewayClient;
import com.streamcell.platform.ai.converter.AIConverter;
import com.streamcell.platform.ai.domain.context.FlinkSQLGenerationContext;
import com.streamcell.platform.ai.domain.context.KafkaSourceDDLGenerationContext;
import com.streamcell.platform.ai.domain.context.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import com.streamcell.platform.ai.domain.generator.PostgreSQLSinkDDLGenerator;
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
import com.streamcell.platform.ai.dto.FlinkSQLGatewayRequest;
import com.streamcell.platform.ai.dto.FlinkSQLGatewayResponse;
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
    private final PostgreSQLSinkDDLGenerator postgreSQLSinkDDLGenerator;

    private final FlinkSQLGatewayClient flinkSQLGatewayClient;

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

    @Override
    public FlinkSQLGatewayResponse.SubmitSQL flinkSqlGatewayTest() throws JsonProcessingException {
        JsonMapper jsonMapper = new JsonMapper();
        String sourceJson = """
                {
                  "sourceTopicId": 1,
                  "window": {
                    "type": "TUMBLE",
                    "size": 5,
                    "unit": "MINUTE"
                  },
                  "groupBy": [
                    "product_id"
                  ],
                  "aggregations": [
                    {
                      "function": "COUNT",
                      "field": "*",
                      "alias": "order_count"
                    },
                    {
                      "function": "AVG",
                      "field": "payment_amount",
                      "alias": "avg_payment_amount"
                    }
                  ],
                  "filters": [
                    {
                      "field": "payment_amount",
                      "operator": "GTE",
                      "value": 10000
                    }
                  ]
                }
                """;

        PipelinePlan pipelinePlan = jsonMapper.readValue(sourceJson, PipelinePlan.class);
        PipelinePlanValidationContext pipelinePlanValidationContext
                = pipelinePlanValidationContextResolver.resolve(1L, 1L, pipelinePlan);

        KafkaSourceDDLGenerationContext kafkaSourceDDLGenerationContext =
                aiConverter.toKafkaSourceDDLGenerationContext(pipelinePlanValidationContext);

        String generatedSql = kafkaSourceDDLGenerator.generate(kafkaSourceDDLGenerationContext);

        FlinkSQLGatewayResponse.CreateSession session = flinkSQLGatewayClient.createSession();
        FlinkSQLGatewayResponse.CreateSource source =
                flinkSQLGatewayClient.createSource(session.getSessionHandle(), FlinkSQLGatewayRequest.CreateSource.from(generatedSql));

        PostgreSQLSinkDDLGenerationContext postgreSQLGenerationContext =
                aiConverter.toPostgreSQLGenerationContext(pipelinePlanValidationContext);
        String generatedSinkSql = postgreSQLSinkDDLGenerator.generate(postgreSQLGenerationContext);

        FlinkSQLGatewayResponse.CreateSink sink =
                flinkSQLGatewayClient.createSink(
                        session.getSessionHandle(), FlinkSQLGatewayRequest.CreateSink.from(generatedSinkSql));


        FlinkSQLGenerationContext flinkSQLGenerationContext
                = aiConverter.toGenerationContext(pipelinePlanValidationContext);
        String generatedFlinkSql = flinkSQLGenerator.generate(flinkSQLGenerationContext);

        FlinkSQLGatewayResponse.SubmitSQL submitSQL =
                flinkSQLGatewayClient.submitSQL(session.getSessionHandle(), FlinkSQLGatewayRequest.SubmitSQL.from(generatedFlinkSql));

        return submitSQL;
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
