package com.streamcell.platform.pipeline.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.global.security.utils.SecurityUtil;
import com.streamcell.platform.ai.domain.policy.PostgreSQLSinkPolicy;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.domain.spec.FilterSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.domain.pipeline.result.ColumnExtractor;
import com.streamcell.platform.pipeline.dto.PipelineResultQueryResponse;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.repository.PipelineResultQueryRepository;
import com.streamcell.platform.pipeline.service.PipelineResultQueryService;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineDeployment;
import com.streamcell.platform.topic.repository.TopicRepository;
import com.streamcell.platform.topic.vo.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineResultQueryServiceImpl implements PipelineResultQueryService {

    private final PipelineRepository pipelineRepository;
    private final PipelineResultQueryRepository pipelineResultQueryRepository;
    private final TopicRepository topicRepository;

    private final ColumnExtractor columnExtractor;

    private final JsonMapper jsonMapper = new JsonMapper();


    @Override
    public PipelineResultQueryResponse.Dashboard getPipelineResults(Long pipelineId, Integer limit, Integer rangeMinutes) {

        Pipeline pipeline = pipelineRepository.findPipelineByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));

        if (PipelineType.AI_SQL != pipeline.getPipelineType()) {
            throw new BaseAPIException(ErrorCode.INVALID_PIPELINE_RESULT_PIPELINE_TYPE);
        }

        // 가장 최근 Deployment만 가져오기
        PipelineDeployment pipelineDeployment = pipelineRepository.findLatestPipelineDeployMentByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE_DEPLOYMENT, pipelineId));

        Topic topic = topicRepository.findByAISqlPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_TOPIC));

        // 소유권한 검증
        Long currentUserId = SecurityUtil.getUserId();
        if (!pipeline.getOwnerUserId().equals(currentUserId)) {
            throw new BaseAPIException(ErrorCode.FORBIDDEN_PIPELINE);
        }

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

        List<PipelineResultQueryResponse.Dashboard.Chart.Series> series = getSeries(pipelinePlan);

        String targetTable = String.format(PostgreSQLSinkPolicy.RESULT_TABLE_NAME_CONVENTION, pipelineId);

        return PipelineResultQueryResponse.Dashboard.builder()
                .pipelineId(pipelineId)
                .deploymentId(pipelineDeployment.getDeploymentId())
                .pipelineStatus(pipeline.getPipelineStatus())
                .columns(columnExtractor.extract(pipelinePlan, topic))
                .rows(pipelineResultQueryRepository.findRowsByTableName(targetTable, limit, rangeMinutes))
                .chart(PipelineResultQueryResponse.Dashboard.Chart.builder()
                        .xKey("window_start")
                        .series(series)
                        .build()
                )
                .build();
    }

    private List<PipelineResultQueryResponse.Dashboard.Chart.Series> getSeries(PipelinePlan pipelinePlan) {
        List<PipelineResultQueryResponse.Dashboard.Chart.Series> series = new ArrayList<>();
        List<String> groupBys = pipelinePlan.getGroupBy();

        List<AggregationSpec> aggregations = pipelinePlan.getAggregations();
        for (AggregationSpec aggregation : aggregations) {
            String alias = aggregation.getAlias();
            series.add(
                    PipelineResultQueryResponse.Dashboard.Chart.Series.builder()
                            .key(alias)
                            .label("test")
                            .groupKeys(groupBys).build()
            );
        }
        return series;
    }
}
