package com.streamcell.platform.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.policy.FlinkSQLPolicy;
import com.streamcell.platform.flink.client.FlinkRestClient;
import com.streamcell.platform.flink.client.FlinkSQLGatewayClient;
import com.streamcell.platform.ai.converter.AIConverter;
import com.streamcell.platform.ai.domain.context.FlinkSQLGenerationContext;
import com.streamcell.platform.ai.domain.context.KafkaSourceDDLGenerationContext;
import com.streamcell.platform.ai.domain.context.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import com.streamcell.platform.ai.domain.generator.PostgreSQLSinkDDLGenerator;
import com.streamcell.platform.ai.domain.manager.PipelineResultTableManager;
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
import com.streamcell.platform.flink.dto.FlinkSQLGatewayRequest;
import com.streamcell.platform.flink.dto.FlinkSQLGatewayResponse;
import com.streamcell.platform.flink.dto.FlinkSQLGatewayResponse.FetchResult;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.dto.PipelineResultTable;
import com.streamcell.platform.ai.enums.ResultType;
import com.streamcell.platform.ai.service.AIService;
import com.streamcell.platform.flink.dto.FlinkSQLGatewayResponse.FetchStatus;
import com.streamcell.platform.flink.enums.FlinkJobStatus;
import com.streamcell.platform.flink.enums.OperationStatus;
import com.streamcell.platform.pipeline.domain.JobStatusConvertPolicy;
import com.streamcell.platform.pipeline.dto.PipelineDeploymentRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.dto.PipelineResponse.Deployment;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.service.PipelineDeploymentService;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AIConverter aiConverter;
    private final PipelinePlanValidationContextResolver pipelinePlanValidationContextResolver;

    private final FlinkSQLGenerator flinkSQLGenerator;
    private final KafkaSourceDDLGenerator kafkaSourceDDLGenerator;
    private final PostgreSQLSinkDDLGenerator postgreSQLSinkDDLGenerator;

    private final PipelineResultTableManager pipelineResultTableManager;

    private final FlinkSQLGatewayClient flinkSQLGatewayClient;
    private final FlinkRestClient flinkRestClient;

    private final JobStatusConvertPolicy jobStatusConvertPolicy;

    private final PipelineDeploymentService pipelineDeploymentService;

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
    public PipelineResponse.Deployment flinkSqlGatewayTest() throws JsonProcessingException {
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
        FlinkSQLGatewayResponse.CreateSource source = flinkSQLGatewayClient.createSource(
            session.getSessionHandle(),
            FlinkSQLGatewayRequest.CreateSource.from(generatedSql));

        PostgreSQLSinkDDLGenerationContext postgreSQLGenerationContext =
                aiConverter.toPostgreSQLGenerationContext(pipelinePlanValidationContext);
        String generatedSinkSql = postgreSQLSinkDDLGenerator.generate(postgreSQLGenerationContext);

        FlinkSQLGatewayResponse.CreateSink sink = flinkSQLGatewayClient.createSink(
            session.getSessionHandle(),
            FlinkSQLGatewayRequest.CreateSink.from(generatedSinkSql));

        PipelineResultTable.Response table =
            pipelineResultTableManager.createTable(postgreSQLGenerationContext);
        log.info("created table name: {}", table.getCreatedTableName());

        FlinkSQLGenerationContext flinkSQLGenerationContext
                = aiConverter.toGenerationContext(pipelinePlanValidationContext);
        String generatedFlinkSql = flinkSQLGenerator.generate(flinkSQLGenerationContext);

        FlinkSQLGatewayResponse.SubmitSQL submitSQL =
                flinkSQLGatewayClient.submitSQL(
                    session.getSessionHandle(),
                    FlinkSQLGatewayRequest.SubmitSQL.from(generatedFlinkSql));

        FetchResult fetchResult = flinkSQLGatewayClient.fetchResult(
            session.getSessionHandle(),
            submitSQL.getOperationHandle());

        fetchResult = checkResultStatusUntilPayloadAndGet(fetchResult, session.getSessionHandle(),
            submitSQL.getOperationHandle());

        String flinkJobId = Optional.ofNullable(fetchResult.getJobId())
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_FLINK_JOB_ID));

        FlinkJobStatus jobStatus = flinkRestClient.getJobStatus(flinkJobId);

        DeploymentStatus deploymentStatus = jobStatusConvertPolicy.convertToDeploymentStatusFrom(jobStatus);

        PipelineDeploymentRequest.Create create = PipelineDeploymentRequest.Create
            .builder()
            .pipelineId(1L)
            .deploymentType(PipelineType.AI_SQL)
            .flinkJobId(flinkJobId)
            .status(deploymentStatus)
            .startedAt(LocalDateTime.now())
            .lastCheckedAt(LocalDateTime.now())
            .build();

        Deployment pipelineDeployment = pipelineDeploymentService.createPipelineDeployment(create);

        return pipelineDeployment;
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

    private FetchResult checkResultStatusUntilPayloadAndGet(FetchResult fetchResult, String sessionHandle, String operationHandle) {
        int maxCount = 20;
        int currentCount = 1;
        int delayMillis = 1000;
        ResultType resultType = fetchResult.getResultType();
        while (currentCount < maxCount) {

            if (ResultType.PAYLOAD == resultType) {
                return fetchResult;
            }

            if (ResultType.EOS == resultType) {
                throw new BaseAPIException(ErrorCode.FAILED_FLINK_SQL_JOB);
            }

            FetchStatus fetchStatus = flinkSQLGatewayClient.fetchStatus(sessionHandle, operationHandle);
            if (OperationStatus.ERROR == fetchStatus.getStatus()
                || OperationStatus.TIMEOUT == fetchStatus.getStatus()
                || OperationStatus.CANCELED == fetchStatus.getStatus()
                || OperationStatus.CLOSED == fetchStatus.getStatus()) {
                throw new BaseAPIException(ErrorCode.FAILED_FLINK_SQL_JOB);
            }

            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            //String nextResultUrl = fetchResult.getNextResultUrl();
            fetchResult = flinkSQLGatewayClient.fetchResult(sessionHandle, operationHandle);
            resultType = fetchResult.getResultType();

            currentCount++;

        }

        if (ResultType.PAYLOAD != resultType) {
            throw new BaseAPIException(ErrorCode.FAILED_FLINK_SQL_JOB);
        }

        return fetchResult;
    }

}
