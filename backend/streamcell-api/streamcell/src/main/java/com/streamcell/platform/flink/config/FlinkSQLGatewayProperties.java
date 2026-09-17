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

    public String getExecuteResultUrl() {
        return baseUrl + "/" + apiVersion + EndPoint.GET_EXECUTE_RESULT.getPath();
    }

    public String getExecuteStatusUrl() {
        return baseUrl + "/" + apiVersion + EndPoint.GET_EXECUTE_STATUS.getPath();
    }
}
