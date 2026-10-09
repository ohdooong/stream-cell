package com.streamcell.web.my.topic.service.impl;

import com.streamcell.global.security.utils.SecurityUtil;
import com.streamcell.platform.topic.converter.TopicConverter;
import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.web.my.topic.repository.MyTopicRepository;
import com.streamcell.web.my.topic.service.MyTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyTopicServiceImpl implements MyTopicService {

    private final MyTopicRepository repository;
    private final TopicConverter topicConverter;

    @Override
    public List<TopicResponse.Item> findTopics() {

        Long currentUserId = SecurityUtil.getUserId();

        return repository.findTopicByUserId(currentUserId)
                .stream()
                .map(topicConverter::toDTO)
                .toList();
    }
}
