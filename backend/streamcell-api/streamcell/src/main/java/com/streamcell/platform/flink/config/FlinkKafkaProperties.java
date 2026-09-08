package com.streamcell.platform.flink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "flink.kafka")
public class FlinkKafkaProperties {
    // flink에서 바라보는 kafka bootstrapServers -> 도커 컨테이너 서비스이름
    private List<String> bootstrapServers;

}
