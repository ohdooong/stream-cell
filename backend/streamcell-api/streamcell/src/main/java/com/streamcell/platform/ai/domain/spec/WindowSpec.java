package com.streamcell.platform.ai.domain.spec;

import com.streamcell.platform.ai.enums.WindowType;
import com.streamcell.platform.ai.enums.WindowUnit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WindowSpec {
    private WindowType type;
    private Integer size;
    private WindowUnit unit;
}
