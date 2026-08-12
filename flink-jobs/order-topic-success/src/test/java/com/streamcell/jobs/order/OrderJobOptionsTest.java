package com.streamcell.jobs.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderJobOptionsTest {
    @Test
    void usesStreamCellLocalDefaults() {
        OrderJobOptions options = OrderJobOptions.fromArgs(new String[0]);

        assertEquals(OrderJobOptions.DEFAULT_BOOTSTRAP_SERVERS, options.bootstrapServers());
        assertEquals("orders", options.topic());
        assertEquals("streamcell-order-topic-success", options.groupId());
        assertEquals("latest", options.startupMode());
    }

    @Test
    void acceptsDeploymentArguments() {
        OrderJobOptions options = OrderJobOptions.fromArgs(new String[]{
                "--bootstrap-servers", "kafka:9092",
                "--topic", "orders-test",
                "--group-id", "orders-test-group",
                "--startup-mode", "EARLIEST"
        });

        assertEquals("kafka:9092", options.bootstrapServers());
        assertEquals("orders-test", options.topic());
        assertEquals("orders-test-group", options.groupId());
        assertEquals("earliest", options.startupMode());
    }

    @Test
    void rejectsUnknownStartupMode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OrderJobOptions.fromArgs(new String[]{"--startup-mode", "committed"})
        );
    }
}
