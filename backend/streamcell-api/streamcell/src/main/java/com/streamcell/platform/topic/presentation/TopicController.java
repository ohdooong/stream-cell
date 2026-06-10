package com.streamcell.platform.topic.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform/topics")
public class TopicController {
    @PostMapping("/sync")
    public ResponseEntity<String> syncTopics() {
        return ResponseEntity.ok("sync completed");
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> helloTest() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "streamcell-api");

        return ResponseEntity.ok(result);
    }
}