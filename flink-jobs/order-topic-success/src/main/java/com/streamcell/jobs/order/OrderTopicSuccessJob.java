package com.streamcell.jobs.order;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public final class OrderTopicSuccessJob {
    private static final String JOB_NAME = "StreamCell Orders Topic Success Job";

    private OrderTopicSuccessJob() {
    }

    public static void main(String[] args) throws Exception {
        OrderJobOptions options = OrderJobOptions.fromArgs(args);

        StreamExecutionEnvironment environment =
                StreamExecutionEnvironment.getExecutionEnvironment();
        environment.getConfig().setGlobalJobParameters(ParameterTool.fromArgs(args));

        KafkaSource<String> source = createSource(options);

        DataStream<String> orders = environment
                .fromSource(source, WatermarkStrategy.noWatermarks(), "orders-kafka-source")
                .uid("orders-kafka-source")
                .filter(OrderTopicSuccessJob::hasText)
                .name("discard-empty-orders")
                .uid("discard-empty-orders");

        // The print sink keeps this example independent of an external sink and makes
        // successfully consumed order events visible in the TaskManager logs.
        orders.print("orders").name("orders-log-sink").uid("orders-log-sink");

        environment.execute(JOB_NAME);
    }

    private static KafkaSource<String> createSource(OrderJobOptions options) {
        KafkaSourceBuilder<String> builder = KafkaSource.<String>builder()
                .setBootstrapServers(options.bootstrapServers())
                .setTopics(options.topic())
                .setGroupId(options.groupId())
                .setValueOnlyDeserializer(new SimpleStringSchema());

        OffsetsInitializer offsetsInitializer = switch (options.startupMode()) {
            case "earliest" -> OffsetsInitializer.earliest();
            case "latest" -> OffsetsInitializer.latest();
            default -> throw new IllegalStateException(
                    "Unexpected startup mode: " + options.startupMode()
            );
        };

        return builder.setStartingOffsets(offsetsInitializer).build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
