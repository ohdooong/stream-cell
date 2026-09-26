package com.streamcell.platform.ai.config;

import com.streamcell.platform.ai.enums.AtlasEndPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "atlas")
public class AtlasProperties {
    private String baseUrl;
    private String agentId;
    private String apiKey;

    public String getCreateSessionUrl() {
        return baseUrl + String.format(AtlasEndPoint.CREATE_SESSION.getPath(), agentId);
    }

}
