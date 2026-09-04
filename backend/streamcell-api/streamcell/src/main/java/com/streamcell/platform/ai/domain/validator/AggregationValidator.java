package com.streamcell.platform.ai.domain.validator;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.enums.AggregationFunction;
import com.streamcell.platform.topic.vo.Topic;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AggregationValidator implements Validator<PipelinePlanValidationContext> {

    private final List<String> numericTypePrefix = List.of("INT", "BIGINT", "FLOAT", "DOUBLE", "DECIMAL");
    private final JsonMapper jsonMapper = new JsonMapper();

    @Override
    public void validate(PipelinePlanValidationContext context) {

        Map<String, Object> parsedTopicSchema = context.getParsedTopicSchema();

        PipelinePlan pipelinePlan = context.getPipelinePlan();
        List<AggregationSpec> aggregations = pipelinePlan.getAggregations();

        // alias 중복검사
        Set<String> seenAlias = new HashSet<>();
        for (AggregationSpec aggregation : aggregations) {
            String targetField = aggregation.getField();

            if (targetField.equals("*") && AggregationFunction.COUNT != aggregation.getFunction()) {
                throw new BaseAPIException(ErrorCode.INVALID_AGGREGATION_FUNCTION);
            }

            String valueType = (String) parsedTopicSchema.get(targetField);

            boolean anyMatch = numericTypePrefix
                .stream()
                .anyMatch(valueType::startsWith);
            if (!anyMatch) {
                throw new BaseAPIException(ErrorCode.NOT_IMPLEMENTED_FIELD_TYPE);
            }

            if (!seenAlias.add(aggregation.getAlias())) {
                throw new BaseAPIException(ErrorCode.CONFLICT_ALIAS, aggregation.getAlias());
            }
        }
    }
}