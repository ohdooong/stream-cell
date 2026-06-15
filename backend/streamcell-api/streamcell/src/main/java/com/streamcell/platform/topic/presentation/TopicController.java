package com.streamcell.platform.topic.presentation;

import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.platform.topic.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Tag(name = "Kafka Topic", description = "Kafka Topic 컨트롤러")
@RestController
@RequestMapping("/api/v1/platform/topic")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService service;

    @Operation(summary = "Kafka Topic 동기화 메서드", description = "Kafka Topic 동기화")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동기화 성공"),
            @ApiResponse(responseCode = "400", description = "Kafka Topic Client Error"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @PostMapping("/sync")
    public ResponseEntity<String> syncTopics() throws Exception {
        service.syncTopics();
        return ResponseEntity.ok("sync completed");
    }

    @Operation(summary = "Topic 조회 메서드", description = "Topic 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @GetMapping("/topics")
    public ResponseEntity<List<TopicResponse.Items>> getTopics() {
        List<TopicResponse.Items> topics = service.getTopics();
        return ResponseEntity.ok(topics);
    }
}