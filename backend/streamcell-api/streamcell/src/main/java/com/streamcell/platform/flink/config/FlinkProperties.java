package com.streamcell.platform.flink.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "flink")
public class FlinkProperties {
    private String baseUrl;

    public String getClusterOverviewUrl() {
        return baseUrl + "/overview";
    }
}
