package com.streamcell.platform.ai.presentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.platform.flink.client.FlinkSQLGatewayClient;
import com.streamcell.platform.ai.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/ai")
@RequiredArgsConstructor
public class AIController {

    private final FlinkSQLGatewayClient flinkSQLGatewayClient;
    private final AIService aiService;

    @PostMapping("/session")
    public ResponseEntity<BaseResponse<?>> getClusterOverview() throws JsonProcessingException {
        return ResponseEntity.ok(BaseResponse.success(aiService.flinkSqlGatewayTest()));
    }
}
