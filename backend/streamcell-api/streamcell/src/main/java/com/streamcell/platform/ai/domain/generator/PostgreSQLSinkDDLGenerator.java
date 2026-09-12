package com.streamcell.platform.ai.domain.generator;

import com.streamcell.global.config.DBConfig;
import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import com.streamcell.platform.ai.domain.policy.FlinkSQLPolicy;
import com.streamcell.platform.ai.domain.policy.PostgreSQLSinkPolicy;
import com.streamcell.platform.ai.domain.resolver.AggregationTypeResolver;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.vo.Pipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostgreSQLSinkDDLGenerator {

    private final DBConfig dbConfig;

    private final AggregationTypeResolver aggregationTypeResolver;

    public String generate(PostgreSQLSinkDDLGenerationContext context) {
        return """
                CREATE TEMPORARY TABLE %s (
                    %s
                ) WITH (
                    %s
                );
                """.formatted(
                        generateSinkTableName(context)
                       ,generateColumns(context)
                       ,generateConnectorOptions(context));
    }

    private String generateSinkTableName(PostgreSQLSinkDDLGenerationContext context) {
        Pipeline pipeline = context.getPipeline();
        return String.format(FlinkSQLPolicy.SINK_TABLE_NAME_CONVENTION, pipeline.getPipelineId());
    }

    private String generateColumns(PostgreSQLSinkDDLGenerationContext context) {
        PipelinePlan pipelinePlan = context.getPipelinePlan();
        Map<String, Object> parsedTopicSchema = context.getParsedTopicSchema();

        List<String> columnItems = new ArrayList<>();

        columnItems.add("window_start TIMESTAMP(3)");
        columnItems.add("window_end TIMESTAMP(3)");

        List<String> groupByItems = pipelinePlan.getGroupBy();
        for (String item : groupByItems) {
            Object schema = parsedTopicSchema.get(item);
            columnItems.add(item + " " + schema);
        }

        List<AggregationSpec> aggregations = pipelinePlan.getAggregations();
        for (AggregationSpec aggregation : aggregations) {
            String field = aggregation.getField();
            String alias = aggregation.getAlias();
            String sinkResultType = aggregationTypeResolver.resolveSinkResultType(
                    aggregation, parsedTopicSchema.getOrDefault(field, "").toString());

            columnItems.add(alias + " " + sinkResultType);
        }

        return String.join(",\n    ", columnItems);
    }

    private String generateConnectorOptions(PostgreSQLSinkDDLGenerationContext context) {
        Pipeline pipeline = context.getPipeline();
        return """
                'connector' = 'jdbc',
                'url' = '%s',
                'table-name' = 'platform.%s',
                'username' = '%s',
                'password' = '%s'
               """.formatted(
                dbConfig.getUrl(),
                String.format(PostgreSQLSinkPolicy.RESULT_TABLE_NAME_CONVENTION, pipeline.getPipelineId()),
                dbConfig.getUsername(),
                dbConfig.getPassword());
    }

}
