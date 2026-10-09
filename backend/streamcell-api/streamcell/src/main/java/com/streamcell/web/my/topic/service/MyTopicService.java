package com.streamcell.web.my.topic.service;

import com.streamcell.platform.topic.dto.TopicResponse;

import java.util.List;

public interface MyTopicService {

    List<TopicResponse.Item> findTopics();
}
