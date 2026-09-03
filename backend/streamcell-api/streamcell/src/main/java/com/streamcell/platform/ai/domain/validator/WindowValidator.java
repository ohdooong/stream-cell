package com.streamcell.platform.ai.domain.validator;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.spec.WindowSpec;
import com.streamcell.platform.ai.enums.WindowType;
import com.streamcell.platform.ai.enums.WindowUnit;

/**
 * AI가 만든 Pipeline Plan이 Window 허용범위인지 검증
 */
public class WindowValidator implements Validator<PipelinePlanValidationContext> {

    @Override
    public void validate(PipelinePlanValidationContext context) {
        WindowSpec window = context.getPipelinePlan().getWindow();

        if (WindowType.TUMBLE != window.getType()) {
            throw new BaseAPIException(ErrorCode.NOT_IMPLEMENTED_WINDOW_TYPE);
        }

        if (window.getSize() <= 0 || window.getSize() > 30) {
            throw new BaseAPIException(ErrorCode.INVALID_WINDOW_SIZE);
        }

        if (WindowUnit.MINUTE != window.getUnit()) {
            throw new BaseAPIException(ErrorCode.NOT_IMPLEMENTED_WINDOW_TYPE);
        }
    }
}
