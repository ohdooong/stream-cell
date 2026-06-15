package com.streamcell.platform.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @apiNote Kafka Topic Client
 */
@Component
@Slf4j
public class KafkaTopicClient {

    private final AdminClient adminClient;

    public KafkaTopicClient(KafkaAdmin kafkaAdmin) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    public Set<String> getTopics() throws ExecutionException, InterruptedException {
        ListTopicsResult topicsResult = adminClient.listTopics();
        return topicsResult.names().get();
    }
}