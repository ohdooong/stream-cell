package com.streamcell.platform.ai.config;

import com.streamcell.platform.ai.enums.AtlasEndPoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "atlas")
public class AtlasProperties {
    private String baseUrl;
    private String agentId;
    private String apiKey;

    public String getCreateSessionUrl() {
        return baseUrl + String.format(AtlasEndPoint.CREATE_SESSION.getPath(), agentId);
    }

    public String getSendMessageUrl(String sessionId) {
        return baseUrl + String.format(AtlasEndPoint.SEND_MESSAGE.getPath(), agentId, sessionId);
    }
}
