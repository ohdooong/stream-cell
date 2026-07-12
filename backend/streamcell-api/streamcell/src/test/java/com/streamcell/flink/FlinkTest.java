package com.streamcell.flink;

import com.streamcell.platform.flink.client.FlinkRestClient;
import com.streamcell.platform.flink.config.FlinkProperties;
import com.streamcell.platform.flink.dto.FlinkResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "flink.base-url=http://localhost:8081"
})
@SpringBootTest(classes = {FlinkRestClient.class, FlinkProperties.class})
@ConfigurationPropertiesScan(basePackages = "com.streamcell.platform")
public class FlinkTest {

    @Autowired
    private FlinkRestClient flinkRestClient;

    @Test
    void Flink_Rest_연동_테스트() {
        FlinkResponse.ClusterOverview clusterOverview = flinkRestClient.getClusterOverview();
        assertThat(clusterOverview).isNotNull();
    }
}

