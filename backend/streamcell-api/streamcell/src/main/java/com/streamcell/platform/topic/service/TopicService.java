package com.streamcell.platform.topic.service;

import com.streamcell.platform.topic.dto.TopicRequest;
import com.streamcell.platform.topic.dto.TopicRequest.Schema;
import com.streamcell.platform.topic.dto.TopicRequest.TopicPermission;
import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.platform.topic.dto.TopicResponse.Item;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Service interface for managing topics and their associated schemas, permissions, and users.
 */
public interface TopicService {

    /**
     * Synchronizes topics with the underlying data source.
     * @throws ExecutionException if an error occurs during the synchronization process.
     * @throws InterruptedException if the synchronization process is interrupted.
     */
    void syncTopics() throws ExecutionException, InterruptedException;

    /**
     * Retrieves a list of topics.
     * @return a list of topic items
     */
    List<Item> getTopics();

    /**
     * Retrieves a topic by its ID.
     * @param topicId the ID of the topic to retrieve
     * @return the topic item
     */
    Item getTopicById(Long topicId);

    /**
     * Updates the schema of a topic.
     * @param topicId the ID of the topic to update
     * @param schema the new schema for the topic
     * @return the number of rows affected by the update
     */
    int updateTopicSchema(Long topicId, Schema schema);

    /**
     * Retrieves a list of permissions associated with a specific topic.
     *
     * @param topicId the ID of the topic whose permissions are to be retrieved
     * @return a list of {@code TopicResponse.TopicPermission} objects representing the permissions of the specified topic
     */
    List<TopicResponse.TopicPermission> getPermissionsOfTopic(Long topicId);

    /**
     * Retrieves a list of permissions associated with a specific user.
     *
     * @param userId the ID of the user whose permissions are to be retrieved
     * @return a list of {@code TopicResponse.TopicPermission} objects representing the permissions of the specified user
     */
    List<TopicResponse.TopicPermission> getPermissionsOfTopicByUserId(Long userId);

    /**
     * Posts a list of permissions for a specific topic.
     *
     * @param topicId the ID of the topic to which the permissions are to be posted
     * @param topicPermission the list of {@code TopicPermission} objects representing the permissions to be posted
     * @return a list of {@code TopicResponse.TopicPermission} objects representing the posted permissions
     */
    List<TopicResponse.TopicPermission> postUsersPermissionsOfTopic(Long topicId, TopicPermission topicPermission);
}
