package com.streamcell.platform.ai.domain.validator;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.vo.Pipeline;

public class PipelineValidator implements Validator<PipelinePlanValidationContext> {

    @Override
    public void validate(PipelinePlanValidationContext context) {
        Pipeline pipeline = context.getPipeline();

        if (pipeline == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE);
        }

        if (PipelineType.AI_SQL != pipeline.getPipelineType()) {
            throw new BaseAPIException(ErrorCode.INVALID_AI_SQL_REQUEST);
        }

        if (!context.getUserId().equals(pipeline.getOwnerUserId())) {
            throw new BaseAPIException(ErrorCode.FORBIDDEN_PIPELINE);
        }

        if (PipelineStatus.CREATED != pipeline.getPipelineStatus()) {
            throw new BaseAPIException(ErrorCode.INVALID_PIPELINE_STATUS_FOR_AI_SQL);
        }

    }
}
