package com.streamcell.platform.ai.domain.generator;

import com.streamcell.platform._common.config.FlinkJdbcConfig;
import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import com.streamcell.platform.ai.domain.policy.FlinkSQLPolicy;
import com.streamcell.platform.ai.domain.policy.PostgreSQLSinkPolicy;
import com.streamcell.platform.ai.domain.resolver.AggregationTypeResolver;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.domain.policy.PipelineResultKeyPolicy;
import com.streamcell.platform.pipeline.vo.Pipeline;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgreSQLSinkDDLGenerator {

    private final FlinkJdbcConfig flinkJdbcConfig;

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

        columnItems.add("window_start TIMESTAMP(3) NOT NULL");
        columnItems.add("window_end TIMESTAMP(3) NOT NULL");

        List<String> groupByItems = pipelinePlan.getGroupBy();
        for (String item : groupByItems) {
            Object schema = parsedTopicSchema.get(item);
            columnItems.add(item + " " + schema + " NOT NULL");
        }

        List<AggregationSpec> aggregations = pipelinePlan.getAggregations();
        for (AggregationSpec aggregation : aggregations) {
            String field = aggregation.getField();
            String alias = aggregation.getAlias();
            String sinkResultType = aggregationTypeResolver.resolveSinkResultType(
                    aggregation, parsedTopicSchema.getOrDefault(field, "").toString());

            columnItems.add(alias + " " + sinkResultType);
        }

//        PRIMARY KEY (
//            window_start,
//            window_end,
//            product_id
//    ) NOT ENFORCED
        List<String> primaryKeyColumns = PipelineResultKeyPolicy.getPrimaryKeyColumns(pipelinePlan);

        columnItems.add(
            "PRIMARY KEY (" +
                String.join(", ", primaryKeyColumns) +
                ") NOT ENFORCED"
        );

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
                flinkJdbcConfig.getUrl(),
                String.format(PostgreSQLSinkPolicy.RESULT_TABLE_NAME_CONVENTION, pipeline.getPipelineId()),
                flinkJdbcConfig.getUsername(),
                flinkJdbcConfig.getPassword());
    }

}
