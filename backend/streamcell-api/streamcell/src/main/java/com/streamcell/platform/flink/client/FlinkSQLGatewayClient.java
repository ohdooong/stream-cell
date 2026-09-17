package com.streamcell.platform.flink.client;

import com.streamcell.platform.flink.dto.FlinkSQLGatewayRequest;
import com.streamcell.platform.flink.dto.FlinkSQLGatewayResponse;
import com.streamcell.platform.flink.config.FlinkSQLGatewayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class FlinkSQLGatewayClient {

    private final FlinkSQLGatewayProperties flinkSQLGatewayProperties;
    private final RestClient restClient;

    public FlinkSQLGatewayResponse.CreateSession createSession() {
        return restClient.post()
                .uri(flinkSQLGatewayProperties.getCreateSessionUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(FlinkSQLGatewayResponse.CreateSession.class);
    }

    public FlinkSQLGatewayResponse.CreateSource createSource(String sessionHandle, FlinkSQLGatewayRequest.CreateSource requestDto) {
        return restClient.post()
                .uri(String.format(flinkSQLGatewayProperties.getCreateSourceOrSinkUrl(), sessionHandle))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDto)
                .retrieve()
                .body(FlinkSQLGatewayResponse.CreateSource.class);
    }

    public FlinkSQLGatewayResponse.CreateSink createSink(String sessionHandle, FlinkSQLGatewayRequest.CreateSink requestDto) {
        return restClient.post()
                .uri(String.format(flinkSQLGatewayProperties.getCreateSourceOrSinkUrl(), sessionHandle))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDto)
                .retrieve()
                .body(FlinkSQLGatewayResponse.CreateSink.class);
    }

    public FlinkSQLGatewayResponse.SubmitSQL submitSQL(String sessionHandle, FlinkSQLGatewayRequest.SubmitSQL requestDto) {
        return restClient.post()
                .uri(String.format(flinkSQLGatewayProperties.getSubmitSQLUrl(), sessionHandle))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDto)
                .retrieve()
                .body(FlinkSQLGatewayResponse.SubmitSQL.class);
    }

    public FlinkSQLGatewayResponse.FetchResult fetchResult(String sessionHandle, String operationHandle) {
        return restClient.get()
            .uri(String.format(flinkSQLGatewayProperties.getExecuteResultUrl(), sessionHandle, operationHandle))
            .retrieve()
            .body(FlinkSQLGatewayResponse.FetchResult.class);
    }

    public FlinkSQLGatewayResponse.FetchStatus fetchStatus(String sessionHandle, String operationHandle) {
        return restClient.get()
            .uri(String.format(flinkSQLGatewayProperties.getExecuteStatusUrl(), sessionHandle, operationHandle))
            .retrieve()
            .body(FlinkSQLGatewayResponse.FetchStatus.class);
    }

    public FlinkSQLGatewayResponse.CloseSession closeSession(String sessionHandle) {
        return null;
    }
}
