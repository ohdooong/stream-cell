package com.streamcell.platform.topic.service.impl;

import com.streamcell.platform.kafka.KafkaTopicClient;
import com.streamcell.platform.topic.converter.TopicConverter;
import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.platform.topic.repository.TopicRepository;
import com.streamcell.platform.topic.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final KafkaTopicClient topicClient;
    private final TopicRepository repository;

    @Override
    public void syncTopics() throws ExecutionException, InterruptedException {
        Set<String> topics = topicClient.getTopics();
        for (String topic : topics) {
            repository.mergeIntoTopic(topic);
        }
    }

    @Override
    public List<TopicResponse.Items> getTopics() {
        return repository.findAll()
                .stream()
                .map(TopicConverter::toDTO)
                .toList();




    }
}
