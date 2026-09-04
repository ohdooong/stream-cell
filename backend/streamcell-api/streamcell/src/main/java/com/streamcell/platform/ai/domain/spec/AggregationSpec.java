package com.streamcell.platform.ai.domain.spec;

import com.streamcell.platform.ai.enums.AggregationFunction;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AggregationSpec {
    private AggregationFunction function;
    private String field;
    private String alias;
}
