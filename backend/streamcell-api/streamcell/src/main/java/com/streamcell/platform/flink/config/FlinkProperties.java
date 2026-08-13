package com.streamcell.platform.flink.config;

import com.streamcell.platform.flink.enums.EndPoint;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "flink")
public class FlinkProperties {
    private String baseUrl;

    public String getClusterOverviewUrl() {
        return baseUrl + EndPoint.OVERVIEW.getPath();
    }

    public String getUploadJarUrl() {
        return baseUrl + EndPoint.UPLOAD_JAR.getPath();
    }

    public String getRunJarUrl() {
        return baseUrl + EndPoint.RUN_JAR.getPath();
    }

    public String getJobStatus() {
        return baseUrl + EndPoint.JOB_STATUS.getPath();
    }

    public String getCancelJobUrl() {
        return baseUrl + EndPoint.CANCEL_JOB.getPath() + "?mode=cancel";
    }

}
