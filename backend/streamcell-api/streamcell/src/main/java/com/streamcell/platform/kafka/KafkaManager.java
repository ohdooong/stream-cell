package com.streamcell.platform.kafka;

import org.springframework.stereotype.Component;

/**
 * KafkaManager
 */
@Component
public class KafkaManager {

    private final KafkaTopicClient kafkaTopicClient;

    public KafkaManager(KafkaTopicClient kafkaTopicClient) {
        this.kafkaTopicClient = kafkaTopicClient;
    }

    
}