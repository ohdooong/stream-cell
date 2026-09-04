package com.streamcell.platform.ai.domain.validator;


import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.spec.FilterSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.enums.FilterOperator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FilterValidator implements Validator<PipelinePlanValidationContext> {

    private final List<String> numericTypePrefix = List.of("INT", "BIGINT", "FLOAT", "DOUBLE", "DECIMAL");

    private final List<FilterOperator> numericOnlyOperators = List.of(
        FilterOperator.GTE,
        FilterOperator.GT,
        FilterOperator.LTE,
        FilterOperator.LT
    );


    @Override
    public void validate(PipelinePlanValidationContext context) {

        Map<String, Object> parsedTopicSchema = context.getParsedTopicSchema();

        PipelinePlan pipelinePlan = context.getPipelinePlan();
        List<FilterSpec> filters = pipelinePlan.getFilters();

        Set<String> schemaKeySet = parsedTopicSchema.keySet();
        for (FilterSpec filter : filters) {
            String field = filter.getField();

            FilterOperator operator = filter.getOperator();
            Object value = filter.getValue();

            // type 교차검증
            String valueType = (String) parsedTopicSchema.get(field);
            boolean numericType = isNumericType(valueType);
            boolean valid;
            if (numericType) {
                valid = value instanceof Number;
            } else {
                valid = !(value instanceof Number);
            }

            if (!valid) {
                throw new BaseAPIException(ErrorCode.INVALID_AI_SQL_REQUEST_FILTER_VALUE_TYPE);
            }

            if (!numericType && numericOnlyOperators.contains(operator)) {
                throw new BaseAPIException(ErrorCode.INVALID_AI_SQL_REQUEST_OPERATOR);
            }

        }
    }


    private boolean isNumericType(String valueType) {
        return numericTypePrefix
            .stream()
            .anyMatch(valueType::startsWith);
    }
}