package com.streamcell.platform.ai.domain.resolver;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.enums.AggregationFunction;
import org.springframework.stereotype.Component;

@Component
public class PostgreSQLTypeResolver {

    public String resolve(String flinkType) {
        if (flinkType == null) {
            throw new BaseAPIException(ErrorCode.NOT_SUPPORTED_DATA_TYPE);
        }

        if (flinkType.startsWith("DECIMAL")) {
            return flinkType.replace("DECIMAL", "NUMERIC");
        }

        if (flinkType.startsWith("TIMESTAMP")) {
            return flinkType;
        }

        return switch (flinkType) {
            case "STRING" -> "VARCHAR";
            case "INT" -> "INTEGER";
            case "BIGINT" -> "BIGINT";
            case "FLOAT" -> "REAL";
            case "DOUBLE" -> "DOUBLE PRECISION";
            case "BOOLEAN" -> "BOOLEAN";
            default -> throw new BaseAPIException(
                    ErrorCode.NOT_SUPPORTED_DATA_TYPE
            );
        };
    }

}
