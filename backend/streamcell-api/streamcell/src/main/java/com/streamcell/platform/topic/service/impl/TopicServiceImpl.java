package com.streamcell.platform.topic.service.impl;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.global._common.util.JsonUtils;
import com.streamcell.platform.kafka.KafkaManager;
import com.streamcell.platform.topic.converter.TopicConverter;
import com.streamcell.platform.topic.dto.TopicRequest.Schema;
import com.streamcell.platform.topic.dto.TopicResponse.Item;
import com.streamcell.platform.topic.repository.TopicRepository;
import com.streamcell.platform.topic.service.TopicService;
import com.streamcell.platform.topic.vo.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final KafkaManager kafkaManager;
    private final TopicRepository repository;

    @Override
    public void syncTopics() throws ExecutionException, InterruptedException {
        Set<String> topics = kafkaManager.getTopics();
        for (String topic : topics) {
            repository.mergeIntoTopic(topic);
        }
    }

    @Override
    public List<Item> getTopics() {
        return repository.findAll()
                .stream()
                .map(TopicConverter::toDTO)
                .toList();
    }

    @Override
    public Item getTopicById(Long topicId) {
        return Optional.ofNullable(repository.findById(topicId))
                .map(TopicConverter::toDTO)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_TOPIC));
    }

    @Override
    public int updateTopicSchema(Long topicId, Schema schema) {
        Topic topic = TopicConverter.toVO(schema, topicId);

        String schemaJson = schema.getSchemaJson();
        String timeField = schema.getTimeField();
        boolean included = JsonUtils.isIncludedFrom(schemaJson, timeField);
        if (!included) {
            throw new BaseAPIException(ErrorCode.INVALID_REQUEST);
        }

        return repository.updateTopicSchema(topic);
    }
}
