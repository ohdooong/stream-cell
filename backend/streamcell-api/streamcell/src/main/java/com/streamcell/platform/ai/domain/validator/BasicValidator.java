package com.streamcell.platform.ai.domain.validator;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.ai.dto.PipelinePlan;

/**
 * 필수요소들이 필수적으로 값이 있는지 간단히 검증
 */
public class BasicValidator implements Validator<PipelinePlanValidationContext> {

    @Override
    public void validate(PipelinePlanValidationContext context) {
        if (context.getPipeline() == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE);
        }

        if (context.getSourceTopic() == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_TOPIC);
        }

        if (context.getTopicPermissions() == null || context.getTopicPermissions().isEmpty()) {
            throw new BaseAPIException(ErrorCode.FORBIDDEN_TOPICS);
        }

        PipelinePlan pipelinePlan = context.getPipelinePlan();
        if (pipelinePlan == null) {
            throw new BaseAPIException(ErrorCode.INVALID_PIPELINE_PLAN);
        }

        if (pipelinePlan.getWindow() == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_WINDOW_SPEC);
        }

        if (pipelinePlan.getWindow().getType() == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_WINDOW_TYPE);
        }

        if (pipelinePlan.getWindow().getUnit() == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_WINDOW_UNIT);
        }

        if (pipelinePlan.getWindow().getSize() == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_WINDOW_SIZE);
        }
    }
}
