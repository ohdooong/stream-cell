package com.streamcell.platform.ai.domain.validator;


import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.topic.enums.MessageFormat;
import com.streamcell.platform.topic.vo.Topic;

public class TopicValidator implements Validator<PipelinePlanValidationContext> {

    @Override
    public void validate(PipelinePlanValidationContext context) {
        Topic topic = context.getSourceTopic();
        if (topic == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_TOPIC);
        }

        if (topic.getSchemaJson() == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_TOPIC_SCHEMA, topic.getTopicId());
        }

        if (MessageFormat.JSON != topic.getMessageFormat()) {
            throw new BaseAPIException(ErrorCode.INVALID_MESSAGE_FORMAT, topic.getMessageFormat());
        }

        if (topic.getTimeField() == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_TOPIC_TIME_FIELD, topic.getTopicId());
        }
    }
}
