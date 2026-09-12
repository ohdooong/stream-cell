package com.streamcell.platform.ai.domain.resolver;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.enums.AggregationFunction;
import org.springframework.stereotype.Component;

@Component
public class AggregationTypeResolver {

    public String resolveSinkResultType(AggregationSpec aggregationSpec, String sourceType) {
        AggregationFunction function = aggregationSpec.getFunction();

        switch (function) {
            case COUNT -> {
                return "BIGINT";
            }
            case SUM, AVG -> {
                if (sourceType.startsWith("DECIMAL")) {
                    throw new BaseAPIException(ErrorCode.NOT_IMPLEMENTED_RESULT_TYPE);
                }
                return sourceType;
            }
            case MIN, MAX -> {
                return sourceType;
            }
            default -> throw new BaseAPIException(ErrorCode.NOT_FOUND_AGGREGATION_FUNCTION);
        }
    }
}
