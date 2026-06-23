package com.streamcell.platform.topic.service;

import com.streamcell.platform.topic.dto.TopicRequest.Schema;
import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.platform.topic.dto.TopicResponse.Item;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface TopicService {

    void syncTopics() throws ExecutionException, InterruptedException;

    List<Item> getTopics();

    Item getTopicById(Long topicId);

    int updateTopicSchema(Long topicId, Schema schema);

    List<TopicResponse.TopicPermission> getPermissionsOfTopic(Long topicId);

    List<TopicResponse.TopicPermission> getPermissionsOfTopicByUserId(Long userId);
}
