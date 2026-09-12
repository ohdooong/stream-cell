package com.streamcell.platform.ai.client;

import com.streamcell.platform.ai.dto.FlinkSQLGatewayResponse;
import com.streamcell.platform.flink.config.FlinkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlinkSQLGatewayClient {

    private final FlinkProperties flinkProperties;

    public FlinkSQLGatewayResponse.CreateSession createSession() {
        return null;
    }

    public FlinkSQLGatewayResponse.CreateSource createSource(String sessionId, String sourceSql) {
        return null;
    }

    public FlinkSQLGatewayResponse.CreateSink createSink(String sessionId, String sinkSql) {
        return null;
    }

    public FlinkSQLGatewayResponse.SubmitSQL submitSQL(String sessionId, String sql) {
        return null;
    }

    public FlinkSQLGatewayResponse.FetchJobId fetchJobId(String sessionId, String operationHandle) {

    }

    public void closeSession(String sessionId) {

    }
}
