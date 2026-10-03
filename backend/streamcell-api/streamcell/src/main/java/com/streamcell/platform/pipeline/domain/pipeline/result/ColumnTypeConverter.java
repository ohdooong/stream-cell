package com.streamcell.platform.pipeline.domain.pipeline.result;

import com.streamcell.platform.ai.domain.enums.AggregationFunction;
import com.streamcell.platform.pipeline.enums.ColumnType;
import org.springframework.stereotype.Component;

import static com.streamcell.platform.pipeline.enums.ColumnType.*;

@Component
public class ColumnTypeConverter {

    public ColumnType convertColumnType(String dataType) {

        if (dataType.startsWith("TIMESTAMP")) {
            return TIMESTAMP;
        }

        if (dataType.startsWith("FLOAT")
                || dataType.startsWith("DOUBLE") || dataType.startsWith("DECIMAL") || dataType.startsWith("INTEGER")) {
            return NUMBER;
        }

        return STRING;
    }
}
