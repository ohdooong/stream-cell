package com.streamcell.platform.topic.converter;

import com.streamcell.platform.topic.dto.TopicRequest;
import com.streamcell.platform.topic.dto.TopicResponse.Item;
import com.streamcell.platform.topic.vo.Topic;

public class TopicConverter {
    public static Item toDTO(Topic topic) {
        return Item.builder()
                .topicId(topic.getTopicId())
                .displayName(topic.getDisplayName())
                .topicName(topic.getTopicName())
                .messageFormat(topic.getMessageFormat())
                .description(topic.getDescription())
                .timeField(topic.getTimeField())
                .schemaJson(topic.getSchemaJson())
                .build();
    }

    public static Topic toVO(TopicRequest.Schema schema, Long topicId) {
        return Topic.builder()
                .topicId(topicId)
                .displayName(schema.getDisplayName())
                .description(schema.getDescription())
                .timeField(schema.getTimeField())
                .schemaJson(schema.getSchemaJson())
                .messageFormat(schema.getMessageFormat())
                .build();
    }
}
