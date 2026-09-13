package com.streamcell.platform.flink.config;

import com.streamcell.platform.flink.enums.EndPoint;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "flink.sql-gateway")
public class FlinkSQLGatewayProperties {
    private String baseUrl;
    private String apiVersion;

    public String getCreateSessionUrl() {
        return baseUrl + "/" + apiVersion + EndPoint.CREATE_SESSION.getPath();
    }

    public String getCreateSourceOrSinkUrl() {
        return baseUrl + "/" + apiVersion + EndPoint.CREATE_SOURCE_OR_SINK.getPath();
    }

    public String getSubmitSQLUrl() {
        return baseUrl + "/" + apiVersion + EndPoint.SUBMIT_SQL.getPath();
    }

}
