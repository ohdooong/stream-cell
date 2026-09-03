package com.streamcell.platform.ai.domain.validator;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform._common.enums.TopicPermissionType;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.topic.vo.TopicPermission;

import java.util.List;

public class TopicPermissionValidator implements Validator<PipelinePlanValidationContext> {

    @Override
    public void validate(PipelinePlanValidationContext context) {
        List<TopicPermission> topicPermissions = context.getTopicPermissions();

        boolean match = topicPermissions.stream()
                .filter(topicPermission -> topicPermission.getTopicId().equals(context.getSourceTopic().getTopicId()))
                .anyMatch(permission ->
                        TopicPermissionType.ADMIN == permission.getTopicPermissionType()
                     || TopicPermissionType.QUERY == permission.getTopicPermissionType());
        if (!match) {
            throw new BaseAPIException(ErrorCode.FORBIDDEN_AI_SQL);
        }


    }
}
