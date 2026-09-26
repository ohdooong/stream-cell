package com.streamcell.platform.ai.presentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.platform.ai.service.AIDeploymentService;
import com.streamcell.platform.flink.client.FlinkSQLGatewayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/ai")
@RequiredArgsConstructor
public class AIController {

}
