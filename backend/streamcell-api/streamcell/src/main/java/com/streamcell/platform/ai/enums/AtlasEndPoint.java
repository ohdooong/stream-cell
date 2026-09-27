package com.streamcell.platform.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
public enum AtlasEndPoint {
    CREATE_SESSION("/agents/%s/sessions", "세션생성 url -> /agents/{agentId}/sessions"),
    SEND_MESSAGE("/agents/%s/sessions/%s/messages", "만들어진 세션에 메시지 전송 url -> /agents/{agentId}/sessions/{sessionId}/messages")
    ;

    private final String path;
    private String description;
}
