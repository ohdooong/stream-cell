package com.streamcell.platform.ai.domain.manager;

import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import com.streamcell.platform.ai.domain.policy.PostgreSQLSinkPolicy;
import com.streamcell.platform.ai.domain.resolver.AggregationTypeResolver;
import com.streamcell.platform.ai.domain.resolver.PostgreSQLTypeResolver;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.dto.PipelineResultTable;
import com.streamcell.platform.ai.repository.PipelineResultRepository;
import com.streamcell.platform.pipeline.vo.Pipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  pipeline result table 생성 manager
 *  pipelinePlan + topicSchema기반으로 생성
 */
@Component
@RequiredArgsConstructor
public class PipelineResultTableManager {

    private final PostgreSQLTypeResolver postgreSQLTypeResolver;
    private final AggregationTypeResolver aggregationTypeResolver;
    private final PipelineResultRepository repository;

    public PipelineResultTable.Response createTable(PostgreSQLSinkDDLGenerationContext context) {
        Pipeline pipeline = context.getPipeline();
        String tableName = String.format(PostgreSQLSinkPolicy.RESULT_TABLE_NAME_CONVENTION, pipeline.getPipelineId());
        List<String> columns = new ArrayList<>();
        columns.add("window_start TIMESTAMP(3)");
        columns.add("window_end TIMESTAMP(3)");

        PipelinePlan pipelinePlan = context.getPipelinePlan();
        Map<String, Object> parsedTopicSchema = context.getParsedTopicSchema();
        List<String> groupByItems = pipelinePlan.getGroupBy();
        for (String item : groupByItems) {
            String flinkType = (String) parsedTopicSchema.get(item);
            String postgresType = postgreSQLTypeResolver.resolve(flinkType);

            columns.add(item + " " + postgresType);
        }

        List<AggregationSpec> aggregations = pipelinePlan.getAggregations();
        for (AggregationSpec aggregation : aggregations) {
            String postgresColumn = aggregation.getAlias();

            String sinkResultType =
                    aggregationTypeResolver.resolveSinkResultType(
                            aggregation, (String) parsedTopicSchema.get(aggregation.getField()));

            String postgresType = postgreSQLTypeResolver.resolve(sinkResultType);

            columns.add(postgresColumn + " " + postgresType);
        }

        PipelineResultTable dto = PipelineResultTable.builder()
                .tableName(tableName)
                .columns(String.join(",\n    ", columns))
                .build();

        repository.createPipelineResultTable(dto);

        return PipelineResultTable.Response.from(tableName);
    }
}