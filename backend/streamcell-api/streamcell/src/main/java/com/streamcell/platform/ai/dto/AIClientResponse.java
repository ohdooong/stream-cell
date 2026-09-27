package com.streamcell.platform.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AIClientResponse {

    @Getter
    @Setter
    public static class ConnectSession {
        @JsonProperty("id")
        private String sessionId;
        @JsonProperty("agent_id")
        private String agentId;
        @JsonProperty("agent_name")
        private String agentName;
        @JsonProperty("title")
        private String title;
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("user_name")
        private String userName;
        @JsonProperty("is_writable")
        private Boolean isWritable;
        @JsonProperty("is_shared")
        private Boolean isShared;
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
        @JsonProperty("mcp_servers")
        private List<Map<String, Object>> mcpServers;
        @JsonProperty("tool_sets")
        private List<Map<String, Object>> toolSets;
        @JsonProperty("sub_agents")
        private List<Map<String, Object>> subAgents;

    }

    @Getter
    @Setter
    @ToString
    public static class Message {
        @JsonProperty("message")
        private String message;
        @JsonProperty("session_id")
        private String sessionId;
        @JsonProperty("timestamp")
        private LocalDateTime timestamp;
        @JsonProperty("processing_time_ms")
        private Long processingTimeMs;
    }

}
