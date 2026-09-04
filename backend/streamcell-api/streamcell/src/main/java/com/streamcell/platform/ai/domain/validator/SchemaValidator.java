package com.streamcell.platform.ai.domain.validator;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.domain.spec.FilterSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.topic.vo.Topic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchemaValidator implements Validator<PipelinePlanValidationContext> {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Override
    public void validate(PipelinePlanValidationContext context) {

        PipelinePlan pipelinePlan = context.getPipelinePlan();
        List<String> planSchemas = new ArrayList<>();
        planSchemas.addAll(pipelinePlan.getGroupBy());
        planSchemas.addAll(pipelinePlan.getAggregations().stream().map(AggregationSpec::getField).toList());
        planSchemas.addAll(pipelinePlan.getFilters().stream().map(FilterSpec::getField).toList());
        planSchemas = planSchemas.stream().distinct().toList();

        Map<String, Object> parsedTopicSchema = context.getParsedTopicSchema();
        Set<String> schemaKeySet = parsedTopicSchema.keySet();
        // plan schema 검증
        if (!schemaKeySet.containsAll(planSchemas)) {
            throw new BaseAPIException(ErrorCode.INVALID_AI_SQL_REQUEST);
        }

        // filter field schema 검증
        boolean notContainField = pipelinePlan.getFilters()
            .stream()
            .anyMatch(filter -> !schemaKeySet.contains(filter.getField()));

        if (notContainField) {
            throw new BaseAPIException(ErrorCode.INVALID_AI_SQL_REQUEST);
        }


    }
}