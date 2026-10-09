package com.streamcell.web.my.topic.presentation;


import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.platform.topic.dto.TopicResponse;
import com.streamcell.web.my.topic.service.MyTopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Web My Topic API", description = "나의 Topic 조회 및 관리 API 컨트롤러")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/web/my/topic")
public class MyTopicController {

    private final MyTopicService service;

    @Operation(summary = "Topic 조회 메서드", description = "사용자 자신에게 허용된 토픽목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @GetMapping("/topics")
    public ResponseEntity<BaseResponse<List<TopicResponse.Item>>> getTopicsByUserId() {
        return ResponseEntity.ok(BaseResponse.success(service.findTopics()));
    }
}
