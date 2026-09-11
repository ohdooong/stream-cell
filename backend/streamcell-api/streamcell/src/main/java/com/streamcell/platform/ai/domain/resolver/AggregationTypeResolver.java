package com.streamcell.platform.ai.domain.resolver;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.enums.AggregationFunction;
import org.springframework.stereotype.Component;

@Component
public class AggregationTypeResolver {

    public String resolveSinkResultType(Object schema) {






        return null;
    }

    public String resolveSinkResultType(AggregationSpec aggregationSpec) {
        AggregationFunction function = aggregationSpec.getFunction();

        switch (function) {
            case COUNT -> {
                return "BIGINT";
            }
            case AVG -> {

            }
            case SUM -> {

            }
            case MAX -> {

            }
            case MIN -> {

            }
            default -> throw new BaseAPIException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return null;
    }
}
