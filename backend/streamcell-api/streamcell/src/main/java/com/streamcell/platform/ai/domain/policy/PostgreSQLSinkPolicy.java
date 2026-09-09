package com.streamcell.platform.ai.domain.policy;

import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostgreSQLSinkPolicy {



    public static List<String> convertToSinkColumns(PipelinePlan pipelinePlan, Map<String, Object> parsedTopicSchema) {
        List<String> columns = new ArrayList<>();

        List<String> groupBys = pipelinePlan.getGroupBy();

        List<AggregationSpec> aggregations = pipelinePlan.getAggregations();


        return columns;
    }

    public String convertToSinkSchema(String original) {



        return null;
    }

}
