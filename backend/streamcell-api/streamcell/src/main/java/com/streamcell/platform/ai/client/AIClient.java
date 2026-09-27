package com.streamcell.platform.ai.client;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.config.AtlasProperties;
import com.streamcell.platform.ai.dto.AIClientRequest;
import com.streamcell.platform.ai.dto.AIClientResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AIClient {

    private final AtlasProperties atlasProperties;
    private final RestClient restClient;

    private static final String API_KEY_HEADER = "x-api-key";

    public AIClientResponse.ConnectSession connect(String title) {

        AIClientRequest.CreateSession requestBody =
                AIClientRequest.CreateSession.from(title == null ? UUID.randomUUID().toString() : title);

        return restClient.post()
                .uri(atlasProperties.getCreateSessionUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, atlasProperties.getApiKey())
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    log.error("세션 요청 API Client 에러발생");
                    log.error("Request Body: {}", requestBody);
                    throw new RuntimeException("세션 요청 API Client 에러발생: " + response.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    log.error("세션 요청 API Server 에러발생");
                    log.error("Response Body: {}", response.getBody());
                    throw new RuntimeException("세션 요청 API Server 에러발생: " + response.getStatusCode());
                })
                .body(AIClientResponse.ConnectSession.class);
    }

    public AIClientResponse.Message sendMessage(String message, AIClientResponse.ConnectSession connectSession) {
        validateConnectedSession(connectSession);

        AIClientRequest.Message requestMessage = AIClientRequest.Message.from(message);

        return restClient.post()
                .uri(atlasProperties.getSendMessageUrl(connectSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, atlasProperties.getApiKey())
                .body(requestMessage)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    log.error("Message API Client 에러발생");
                    log.error("Request Body: {}", requestMessage);
                    throw new RuntimeException("Message API Client 에러발생: " + response.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    log.error("Message API Server 에러발생");
                    log.error("Response Body: {}", response.getBody());
                    throw new RuntimeException("Message API Server 에러발생: " + response.getStatusCode());
                })
                .body(AIClientResponse.Message.class);
    }

    private void validateConnectedSession(AIClientResponse.ConnectSession connectSession) {
        if (connectSession == null || connectSession.getSessionId() == null) {
            throw new BaseAPIException(ErrorCode.NOT_FOUND_AI_AGENT_SESSION);
        }

        String targetAgentId = connectSession.getAgentId();
        String sourceAgentId = atlasProperties.getAgentId();
        if (!sourceAgentId.equals(targetAgentId)) {
            throw new BaseAPIException(ErrorCode.CONFLICT_AI_AGENT_ID);
        }
    }

}