package com.streamcell.platform.topic.converter;

import com.streamcell.platform.topic.dto.TopicRequest;
import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.platform.topic.dto.TopicResponse.Item;
import com.streamcell.platform.topic.vo.Topic;
import com.streamcell.platform.topic.vo.TopicPermission;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TopicConverter {
    Item toDTO(Topic topic);

    default Topic toVO(TopicRequest.Schema schema, Long topicId) {
        return Topic.builder()
                .topicId(topicId)
                .displayName(schema.getDisplayName())
                .description(schema.getDescription())
                .timeField(schema.getTimeField())
                .schemaJson(schema.getSchemaJson())
                .messageFormat(schema.getMessageFormat())
                .build();
    }

    Topic toVO(Item item);

    TopicResponse.TopicPermission toDTO(TopicPermission topicPermission);
}
