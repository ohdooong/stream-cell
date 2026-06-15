package com.streamcell.platform.topic.converter;

import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.platform.topic.vo.Topic;

public class TopicConverter {
    public static TopicResponse.Items toDTO(Topic topic) {
        return TopicResponse.Items.builder()
                .topicId(topic.getTopicId())
                .displayName(topic.getDisplayName())
                .topicName(topic.getTopicName())
                .messageFormat(topic.getMessageFormat())
                .description(topic.getDescription())
                .timeField(topic.getTimeField())
                .schemaJson(topic.getSchemaJson())
                .build();
    }
}
