package com.streamcell.jobs.order;

import org.apache.flink.api.java.utils.ParameterTool;

import java.util.Locale;
import java.util.Set;

record OrderJobOptions(
        String bootstrapServers,
        String topic,
        String groupId,
        String startupMode
) {
    static final String DEFAULT_BOOTSTRAP_SERVERS =
            "kafka-1:9092,kafka-2:9092,kafka-3:9092";
    static final String DEFAULT_TOPIC = "orders";
    static final String DEFAULT_GROUP_ID = "streamcell-order-topic-success";
    static final String DEFAULT_STARTUP_MODE = "latest";

    private static final Set<String> STARTUP_MODES = Set.of("earliest", "latest");

    static OrderJobOptions fromArgs(String[] args) {
        ParameterTool parameters = ParameterTool.fromArgs(args);

        String bootstrapServers = requireText(
                "bootstrap-servers",
                parameters.get("bootstrap-servers", DEFAULT_BOOTSTRAP_SERVERS)
        );
        String topic = requireText("topic", parameters.get("topic", DEFAULT_TOPIC));
        String groupId = requireText("group-id", parameters.get("group-id", DEFAULT_GROUP_ID));
        String startupMode = requireText(
                "startup-mode",
                parameters.get("startup-mode", DEFAULT_STARTUP_MODE)
        ).toLowerCase(Locale.ROOT);

        if (!STARTUP_MODES.contains(startupMode)) {
            throw new IllegalArgumentException(
                    "startup-mode must be one of " + STARTUP_MODES + ", but was: " + startupMode
            );
        }

        return new OrderJobOptions(bootstrapServers, topic, groupId, startupMode);
    }

    private static String requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
