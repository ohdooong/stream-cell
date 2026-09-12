package com.streamcell.platform.ai.domain.generator;

import com.streamcell.platform.ai.domain.context.FlinkSQLGenerationContext;
import com.streamcell.platform.ai.domain.policy.FlinkSQLPolicy;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.domain.spec.FilterSpec;
import com.streamcell.platform.ai.domain.spec.WindowSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.enums.FilterOperator;
import com.streamcell.platform.ai.enums.WindowType;
import com.streamcell.platform.ai.enums.WindowUnit;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.topic.vo.Topic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FlinkSQLGenerator {
    public String generate(FlinkSQLGenerationContext context) {
        return generateInsertClause(context) + "\n"
             + generateSelectClause(context) + "\n"
             + generateFromClause(context) + "\n"
             + generateWhereClause(context) + "\n"
             + generateGroupByClause(context) + ";";
    }

    private String generateInsertClause(FlinkSQLGenerationContext context) {
        Pipeline pipeline = context.getPipeline();
        Long pipelineId = pipeline.getPipelineId();
        return "INSERT INTO " + String.format(FlinkSQLPolicy.SINK_TABLE_NAME_CONVENTION, pipelineId);
    }

    private String generateSelectClause(FlinkSQLGenerationContext context) {
        PipelinePlan pipelinePlan = context.getPipelinePlan();
        List<String> selectItems = new ArrayList<>();

        selectItems.add("window_start");
        selectItems.add("window_end");

        // groupBy
        List<String> groupBys = pipelinePlan.getGroupBy();
        if (groupBys != null && !groupBys.isEmpty()) {
            selectItems.addAll(groupBys);
        }

        // aggregation
        selectItems.addAll(generateAggregation(context));

        return "SELECT\n    "
            + String.join(",\n    ", selectItems);
    }

    private String generateFromClause(FlinkSQLGenerationContext context) {

        Topic topic = context.getSourceTopic();
        Pipeline pipeline = context.getPipeline();
        PipelinePlan pipelinePlan = context.getPipelinePlan();

        WindowSpec window = pipelinePlan.getWindow();
        WindowType windowType = window.getType();
        WindowUnit windowUnit = window.getUnit();

        return "FROM TABLE(\n    "
                    + windowType.name() + "(\n        "
                        + "TABLE " + String.format(FlinkSQLPolicy.SOURCE_TABLE_NAME_CONVENTION
                                                 , pipeline.getPipelineId()
                                                 , topic.getTopicId()) + ",\n        "
                        + "DESCRIPTOR" + "(" + topic.getTimeField() + "),\n        "
                        + "INTERVAL " + "'" + window.getSize() + "'" + " " + windowUnit.name() + "\n    "
                    + ")\n"
             + ")";
    }

    private String generateWhereClause(FlinkSQLGenerationContext context) {
        List<FilterSpec> filters = context.getPipelinePlan().getFilters();
        if (filters == null || filters.isEmpty()) {
            return "";
        }

        List<String> conditions = new ArrayList<>();
        for (FilterSpec filter : filters) {
            conditions.add(generateFilter(filter));
        }

        return "WHERE " + String.join("\n    AND ", conditions);
    }

    private String generateGroupByClause(FlinkSQLGenerationContext context) {
        List<String> groupByItems = new ArrayList<>();
        groupByItems.add("window_start");
        groupByItems.add("window_end");

        PipelinePlan pipelinePlan = context.getPipelinePlan();
        List<String> groupBys = Optional.ofNullable(pipelinePlan.getGroupBy())
            .orElseGet(Collections::emptyList);
        groupByItems.addAll(groupBys);

        return "GROUP BY\n    "
            + String.join(",\n    ", groupByItems);
    }

    private List<String> generateAggregation(FlinkSQLGenerationContext context) {
        PipelinePlan pipelinePlan = context.getPipelinePlan();
        List<AggregationSpec> aggregations = pipelinePlan.getAggregations();
        List<String> selectItems = new ArrayList<>();
        for (AggregationSpec aggregation : aggregations) {
            String item = aggregation.getFunction().name() + "(" + aggregation.getField() + ")"
                + " AS " + aggregation.getAlias();

            selectItems.add(item);
        }
        return selectItems;
    }

    private String generateFilter(FilterSpec filter) {
        String field = filter.getField();
        FilterOperator operator = filter.getOperator();
        Object value = filter.getValue();

        return field + " " + operator.getFormula() + " " + (!(value instanceof Number)
            ? String.format("'%s'", value.toString().replace("'", "''")) : value);
    }
}
