package com.streamcell.platform._common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "flink.jdbc")
@Getter
@Setter
public class FlinkJdbcConfig {
    private String url;
    private String username;
    private String password;
}
