package com.streamcell.platform.topic.service.impl;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.global._common.util.JsonUtils;
import com.streamcell.platform._common.enums.TopicPermissionType;
import com.streamcell.platform._common.port.UserLookupPort;
import com.streamcell.platform.kafka.KafkaManager;
import com.streamcell.platform.topic.converter.TopicConverter;
import com.streamcell.platform.topic.dto.TopicRequest.Schema;
import com.streamcell.platform.topic.dto.TopicRequest.TopicPermission;
import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.platform.topic.dto.TopicResponse.Item;
import com.streamcell.platform.topic.repository.TopicRepository;
import com.streamcell.platform.topic.service.TopicService;
import com.streamcell.platform.topic.vo.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final KafkaManager kafkaManager;
    private final TopicRepository repository;

    private final UserLookupPort userLookupPort;

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
        return repository.findById(topicId)
                .map(TopicConverter::toDTO)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_TOPIC));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    public List<TopicResponse.TopicPermission> getPermissionsOfTopic(Long topicId) {
        return repository.findTopicPermissions(topicId)
                .stream()
                .map(TopicConverter::toDTO)
                .toList();
    }

    @Override
    public List<TopicResponse.TopicPermission> getPermissionsOfTopicByUserId(Long userId) {
        return repository.findTopicPermissionByUserId(userId)
                .stream()
                .map(TopicConverter::toDTO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TopicResponse.TopicPermission> postUsersPermissionsOfTopic(Long topicId, TopicPermission topicPermission) {
        Topic topic = repository.findById(topicId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_TOPIC));

        List<Long> userIds = topicPermission.getUserIds().stream().distinct().toList();
        validateUsers(userIds);

        for (Long userId : userIds) {
            repository.mergeIntoTopicPermission(topicId, userId);
        }

        return getPermissionsOfTopic(topicId);
    }

    @Override
    public boolean hasPermission(Long userId, List<Long> topicIds, TopicPermissionType topicPermissionType) {
        List<Long> topicsByUserId = repository.findTopicPermissionByUserId(userId)
                .stream()
                .map(com.streamcell.platform.topic.vo.TopicPermission::getTopicId)
                .toList();

        return new HashSet<>(topicsByUserId).containsAll(topicIds);
    }

    private void validateUsers(List<Long> userIds) {
        List<Long> existingUserIds = userLookupPort.findExistingUserIds(userIds);
        if (userIds.size() != existingUserIds.size()) {
            throw new BaseAPIException(ErrorCode.INVALID_USER);
        }
    }

}
