package com.streamcell.platform.topic.service;

import com.streamcell.platform.topic.dto.TopicResponse;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface TopicService {

    void syncTopics() throws ExecutionException, InterruptedException;

    List<TopicResponse.Items> getTopics();
}
