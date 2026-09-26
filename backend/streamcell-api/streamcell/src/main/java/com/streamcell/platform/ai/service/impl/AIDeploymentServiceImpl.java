package com.streamcell.platform.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.client.AIClient;
import com.streamcell.platform.ai.dto.AIDeploymentResponse;
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
import com.streamcell.platform.ai.domain.enums.ResultType;
import com.streamcell.platform.ai.service.AIDeploymentService;
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
public class AIDeploymentServiceImpl implements AIDeploymentService {

    private final AIConverter aiConverter;
    private final PipelinePlanValidationContextResolver pipelinePlanValidationContextResolver;

    private final FlinkSQLGenerator flinkSQLGenerator;
    private final KafkaSourceDDLGenerator kafkaSourceDDLGenerator;
    private final PostgreSQLSinkDDLGenerator postgreSQLSinkDDLGenerator;

    private final PipelineResultTableManager pipelineResultTableManager;

    private final AIClient aiClient;
    private final FlinkSQLGatewayClient flinkSQLGatewayClient;
    private final FlinkRestClient flinkRestClient;

    private final JobStatusConvertPolicy jobStatusConvertPolicy;

    private final JsonMapper jsonMapper = new JsonMapper();

    public void requestPipelinePlan() {
        PipelinePlanValidationContext validationContext =
            validateForPipelinePlan(1L, new PipelinePlan());

        FlinkSQLGenerationContext generationContext =
            aiConverter.toGenerationContext(validationContext);

        String generate = flinkSQLGenerator.generate(generationContext);

        KafkaSourceDDLGenerationContext kafkaSourceDDLGenerationContext
                = aiConverter.toKafkaSourceDDLGenerationContext(validationContext);

        String generate1 = kafkaSourceDDLGenerator.generate(kafkaSourceDDLGenerationContext);
    }

    public PipelineResponse.Deployment flinkSqlGatewayTest() throws JsonProcessingException {
        PipelinePlan pipelinePlan = getPipelinePlanByPipelineId(1L).getPipelinePlan();

        PipelinePlanValidationContext pipelinePlanValidationContext
                = pipelinePlanValidationContextResolver.resolve(1L, 1L, pipelinePlan);
        // kafka source ddl generate
        KafkaSourceDDLGenerationContext kafkaSourceDDLGenerationContext =
                aiConverter.toKafkaSourceDDLGenerationContext(pipelinePlanValidationContext);
        String generatedSql = kafkaSourceDDLGenerator.generate(kafkaSourceDDLGenerationContext);

        // flinkSQL 세션 생성
        FlinkSQLGatewayResponse.CreateSession session = flinkSQLGatewayClient.createSession();
        // kafka source 생성
        FlinkSQLGatewayResponse.CreateSource source = flinkSQLGatewayClient.createSource(
            session.getSessionHandle(),
            FlinkSQLGatewayRequest.CreateSource.from(generatedSql));

        // sink sql generation context 변환
        PostgreSQLSinkDDLGenerationContext postgreSQLGenerationContext =
                aiConverter.toPostgreSQLGenerationContext(pipelinePlanValidationContext);
        // sink sql generate
        String generatedSinkSql = postgreSQLSinkDDLGenerator.generate(postgreSQLGenerationContext);
        // sink 생성
        FlinkSQLGatewayResponse.CreateSink sink = flinkSQLGatewayClient.createSink(
            session.getSessionHandle(),
            FlinkSQLGatewayRequest.CreateSink.from(generatedSinkSql));

        // 실제 결과 result 테이블 생성
        PipelineResultTable.Response table =
            pipelineResultTableManager.createTable(postgreSQLGenerationContext);
        log.info("created table name: {}", table.getCreatedTableName());

        // flink sql 생성 후 배포
        FlinkSQLGenerationContext flinkSQLGenerationContext
                = aiConverter.toGenerationContext(pipelinePlanValidationContext);
        String generatedFlinkSql = flinkSQLGenerator.generate(flinkSQLGenerationContext);
        FlinkSQLGatewayResponse.SubmitSQL submitSQL =
                flinkSQLGatewayClient.submitSQL(
                    session.getSessionHandle(),
                    FlinkSQLGatewayRequest.SubmitSQL.from(generatedFlinkSql));

        // 배포한 sql job 상태가져온 후 pipeline deployment 생성
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

        return null;
    }

    @Override
    public AIDeploymentResponse.GeneratePlan getPipelinePlanByPipelineId(Long pipelineId) {
        // 사용자 자연여 요청 메세지 가져오기
        // AI Agent에게 메세지 요청




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

        PipelinePlan pipelinePlan;
        try {
            pipelinePlan = jsonMapper.readValue(sourceJson, PipelinePlan.class);

        } catch (Exception e) {
            log.error(ErrorCode.JSON_PARSE_ERROR.getMessage() + " : " + e.getMessage());
            throw new BaseAPIException(ErrorCode.JSON_PARSE_ERROR);
        }

        PipelinePlanValidationContext planValidationContext = validateForPipelinePlan(pipelineId, pipelinePlan);
        // 기본 pipeline 가져오기
        // topic metadata 가져오기
        // 사용자가 등록한 AI SQL정보 가져오기
        return AIDeploymentResponse.GeneratePlan.from(pipelinePlan, planValidationContext);
    }

    private PipelinePlanValidationContext validateForPipelinePlan(Long pipelineId, PipelinePlan pipelinePlan) {
        PipelinePlanValidationContext context =
                pipelinePlanValidationContextResolver.resolve(1L, pipelineId, pipelinePlan);  // TODO userId 1L로 고정해놓았지만 수정무조건 필요함!!

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
