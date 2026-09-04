package com.streamcell.platform.ai.domain.spec;

import com.streamcell.platform.ai.enums.FilterOperator;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterSpec {
    private String field;
    private FilterOperator operator;
    private Object value;
}
