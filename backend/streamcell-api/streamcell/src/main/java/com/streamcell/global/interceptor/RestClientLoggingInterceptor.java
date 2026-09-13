package com.streamcell.global.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        logRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        logResponse(response);
        return response;
    }

    private void logRequest(HttpRequest request, byte[] body) {
        log.info("====== HTTP Request Start ======");
        log.info("--> URI: {}", request.getURI());
        log.info("--> Method: {}", request.getMethod());
        log.info("--> Headers: {}", request.getHeaders());
        log.info("--> Body: {}", new String(body, StandardCharsets.UTF_8));
        log.info("====== HTTP Request End ======");
    }

    private void logResponse(ClientHttpResponse response) throws IOException {

        log.info("====== HTTP Response Start ======");
        String body = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        log.info("--> Status: {}", response.getStatusCode());
        log.info("--> Headers: {}", response.getHeaders());
        log.info("--> Body: {}", body);
        log.info("====== HTTP Response End ======");
    }
}
