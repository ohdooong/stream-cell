package com.streamcell.platform.ai.domain.generator;

import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import com.streamcell.platform.ai.domain.policy.FlinkSQLPolicy;
import com.streamcell.platform.ai.domain.policy.PostgreSQLSinkPolicy;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.enums.AggregationFunction;
import com.streamcell.platform.pipeline.vo.Pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostgreSQLSinkDDLGenerator {

    public String generate(PostgreSQLSinkDDLGenerationContext context) {
        return """
                CREATE TEMPORARY TABLE %s (
                    %s
                )
                """.formatted(
                        generateSinkTableName(context)
                      , generateColumns(context));
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


        List<String> groupBys = pipelinePlan.getGroupBy();


        List<AggregationSpec> aggregations = pipelinePlan.getAggregations();
        for (AggregationSpec aggregation : aggregations) {
            String field = aggregation.getField();
            if (AggregationFunction.COUNT == aggregation.getFunction()) {
                columnItems.add(field + " " + "BIGINT");
                continue;
            }

            // COUNT제외 나머지는 원본타입 그대로
            columnItems.add(field + " " + parsedTopicSchema.get(field));
        }


        List<String> converted =
                PostgreSQLSinkPolicy.convertToSinkColumns(pipelinePlan, parsedTopicSchema);
        columnItems.addAll(converted);

        return null;
    }

    private String generateConnectorOptions(PostgreSQLSinkDDLGenerationContext context) {
        return null;
    }

}
