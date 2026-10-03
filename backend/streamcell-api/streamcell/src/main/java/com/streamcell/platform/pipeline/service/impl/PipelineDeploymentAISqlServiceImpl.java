package com.streamcell.platform.pipeline.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.context.FlinkSQLGenerationContext;
import com.streamcell.platform.ai.domain.context.KafkaSourceDDLGenerationContext;
import com.streamcell.platform.ai.domain.context.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import com.streamcell.platform.ai.domain.enums.ResultType;
import com.streamcell.platform.ai.domain.generator.FlinkSQLGenerator;
import com.streamcell.platform.ai.domain.generator.KafkaSourceDDLGenerator;
import com.streamcell.platform.ai.domain.generator.PostgreSQLSinkDDLGenerator;
import com.streamcell.platform.ai.domain.manager.PipelineResultTableManager;
import com.streamcell.platform.ai.domain.resolver.PipelinePlanValidationContextResolver;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.dto.PipelineResultTable;
import com.streamcell.platform.ai.service.AIDeploymentService;
import com.streamcell.platform.flink.client.FlinkRestClient;
import com.streamcell.platform.flink.client.FlinkSQLGatewayClient;
import com.streamcell.platform.flink.dto.FlinkSQLGatewayRequest;
import com.streamcell.platform.flink.dto.FlinkSQLGatewayResponse;
import com.streamcell.platform.flink.enums.FlinkJobStatus;
import com.streamcell.platform.flink.enums.OperationStatus;
import com.streamcell.platform.pipeline.converter.PipelineDeploymentConverter;
import com.streamcell.platform.pipeline.domain.policy.DeploymentStatusPolicy;
import com.streamcell.platform.pipeline.domain.policy.JobStatusConvertPolicy;
import com.streamcell.platform.pipeline.domain.policy.PipelineDeploymentPolicy;
import com.streamcell.platform.pipeline.dto.PipelineDeploymentRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.service.PipelineDeploymentService;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.topic.service.TopicService;
import com.streamcell.platform.topic.vo.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service("pipelineDeploymentAISqlService")
@Slf4j
@RequiredArgsConstructor
public class PipelineDeploymentAISqlServiceImpl implements PipelineDeploymentService {

    private final PipelineRepository repository;
    private final PipelineDeploymentConverter converter;
    private final JsonMapper jsonMapper = new JsonMapper();

    private final TopicService topicService;
    private final AIDeploymentService aiDeploymentService;

    private final PipelinePlanValidationContextResolver pipelinePlanValidationContextResolver;

    // result table manager
    private final PipelineResultTableManager pipelineResultTableManager;

    // generator
    private final FlinkSQLGenerator flinkSQLGenerator;
    private final KafkaSourceDDLGenerator kafkaSourceDDLGenerator;
    private final PostgreSQLSinkDDLGenerator postgreSQLSinkDDLGenerator;

    // flink
    private final FlinkRestClient flinkRestClient;
    private final FlinkSQLGatewayClient flinkSQLGatewayClient;

    // policy
    private final JobStatusConvertPolicy jobStatusConvertPolicy;
    private final DeploymentStatusPolicy deploymentStatusPolicy;
    private final PipelineDeploymentPolicy pipelineDeploymentPolicy;

    @Override
    // @Transactional(rollbackFor = Exception.class) // todo 나중에 flink 외부호출과 연계하여 수정예정
    public PipelineResponse.Deployment deploy(Long pipelineId) {

        Pipeline pipeline = repository.findPipelineByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));

        if (PipelineType.AI_SQL != pipeline.getPipelineType()) {
            throw new BaseAPIException(ErrorCode.INVALID_AI_SQL_REQUEST);
        }

        Topic topic = topicService.getTopicOfAISqlPipelineById(pipelineId);

        String pipelinePlanJson = pipeline.getPipelinePlanJson();
        if (pipelinePlanJson.isBlank()) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE_PLAN_JSON);
        }

        PipelinePlan pipelinePlan;
        try {
            pipelinePlan = jsonMapper.readValue(pipelinePlanJson, PipelinePlan.class);
        } catch (JsonProcessingException e) {
            throw new BaseAPIException(ErrorCode.JSON_PARSE_ERROR);
        }
        PipelinePlanValidationContext planValidationContext = aiDeploymentService.validatePipelinePlan(pipelineId, pipelinePlan);

        boolean canDeploy = pipelineDeploymentPolicy.isDeployPipeline(pipeline.getPipelineStatus());
        if (!canDeploy) {
            throw new BaseAPIException(ErrorCode.INVALID_PIPELINE_STATUS_FOR_AI_SQL, pipeline.getPipelineStatus().name());
        }
        repository.updatePipelineStatusByPipelineIdAndStatus(pipelineId, PipelineStatus.DEPLOYING);

        // context
        KafkaSourceDDLGenerationContext sourceDDLGenerationContext =
                converter.toKafkaSourceDDLGenerationContext(planValidationContext);
        PostgreSQLSinkDDLGenerationContext sinkDDLGenerationContext
                = converter.toPostgreSQLGenerationContext(planValidationContext);
        FlinkSQLGenerationContext flinkSQLGenerationContext =
                converter.toGenerationContext(planValidationContext);

        String flinkJobId;
        FlinkJobStatus jobStatus;
        String generatedFlinkSQL;
        try {
            // 1. 실제 결과 result 테이블 생성
            PipelineResultTable.Response table =
                    pipelineResultTableManager.createTable(sinkDDLGenerationContext);
            log.info("created table name: {}", table.getCreatedTableName());

            // 2. flink sql gateway 세션 생성
            FlinkSQLGatewayResponse.CreateSession session = flinkSQLGatewayClient.createSession();
            // kafka source ddl SQL문 생성
            String generatedSourceDDL = kafkaSourceDDLGenerator.generate(sourceDDLGenerationContext);
            // 3. kafka source 생성 ddl submit
            FlinkSQLGatewayResponse.CreateSource source = flinkSQLGatewayClient.createSource(
                    session.getSessionHandle(),
                    FlinkSQLGatewayRequest.CreateSource.from(generatedSourceDDL));

            // sink 생성 ddl SQL문 생성
            String generatedSinkDDL = postgreSQLSinkDDLGenerator.generate(sinkDDLGenerationContext);
            // 4. sink 생성 ddl SQL문 submit
            FlinkSQLGatewayResponse.CreateSink sink = flinkSQLGatewayClient.createSink(
                    session.getSessionHandle(),
                    FlinkSQLGatewayRequest.CreateSink.from(generatedSinkDDL));


            // Flink SQL문 생성
            generatedFlinkSQL = flinkSQLGenerator.generate(flinkSQLGenerationContext);
            // 5. 생성된 Flink SQL문 submit
            FlinkSQLGatewayResponse.SubmitSQL submitSQL =
                    flinkSQLGatewayClient.submitSQL(
                            session.getSessionHandle(),
                            FlinkSQLGatewayRequest.SubmitSQL.from(generatedFlinkSQL));

            // 배포한 sql job 상태 조회
            FlinkSQLGatewayResponse.FetchResult fetchResult = flinkSQLGatewayClient.fetchResult(
                    session.getSessionHandle(),
                    submitSQL.getOperationHandle());
            fetchResult = checkResultStatusUntilPayloadAndGet(fetchResult, session.getSessionHandle(),
                    submitSQL.getOperationHandle());
            flinkJobId = Optional.ofNullable(fetchResult.getJobId())
                    .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_FLINK_JOB_ID));

            // 실제 flink rest api로 상태한번 더 확인.
            jobStatus = flinkRestClient.getJobStatus(flinkJobId);
        } catch (Exception e) {
            repository.updatePipelineStatusByPipelineIdAndStatus(pipelineId, PipelineStatus.FAILED);
            throw new BaseAPIException(ErrorCode.FAILED_AI_SQL_DEPLOY);
        }

        DeploymentStatus deploymentStatus = jobStatusConvertPolicy.convertToDeploymentStatusFrom(jobStatus);
        PipelineStatus pipelineStatus = jobStatusConvertPolicy.convertToPipelineStatusFrom(jobStatus);

        // pipeline deployment insert
        PipelineDeploymentRequest.Create create = PipelineDeploymentRequest.Create
                .builder()
                .pipelineId(pipelineId)
                .deploymentType(PipelineType.AI_SQL)
                .flinkJobId(flinkJobId)
                .status(deploymentStatus)
                .startedAt(LocalDateTime.now())
                .lastCheckedAt(LocalDateTime.now())
                .build();

        // pipeline deployment insert
        PipelineResponse.Deployment pipelineDeployment = createPipelineDeployment(create);

        // pipeline status 최종 update
        repository.updatePipelineStatusByPipelineIdAndStatus(pipelineId, pipelineStatus);
        // generated sql update
        repository.updatePipelineGeneratedSql(pipelineId, generatedFlinkSQL);

        return pipelineDeployment;
    }

    private FlinkSQLGatewayResponse.FetchResult checkResultStatusUntilPayloadAndGet(FlinkSQLGatewayResponse.FetchResult fetchResult, String sessionHandle, String operationHandle) {
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

            FlinkSQLGatewayResponse.FetchStatus fetchStatus = flinkSQLGatewayClient.fetchStatus(sessionHandle, operationHandle);
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

    /**********  Getter  *********/
    @Override
    public PipelineRepository getPipelineRepository() {
        return this.repository;
    }

    @Override
    public PipelineDeploymentConverter getPipelineDeploymentConverter() {
        return this.converter;
    }

    @Override
    public FlinkRestClient getFlinkRestClient() {
        return this.flinkRestClient;
    }

    @Override
    public JobStatusConvertPolicy getJobStatusConvertPolicy() {
        return this.jobStatusConvertPolicy;
    }

    @Override
    public DeploymentStatusPolicy getDeploymentStatusPolicy() {
        return this.deploymentStatusPolicy;
    }
}
