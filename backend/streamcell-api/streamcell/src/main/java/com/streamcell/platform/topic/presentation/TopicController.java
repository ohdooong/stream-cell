package com.streamcell.platform.topic.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.platform.topic.dto.TopicRequest;
import com.streamcell.platform.topic.dto.TopicResponse.Item;
import com.streamcell.platform.topic.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ResponseEntity<BaseResponse<String>> syncTopics() throws Exception {
        service.syncTopics();
        return ResponseEntity.ok(BaseResponse.success("sync completed"));
    }

    @Operation(summary = "Topic 조회 메서드", description = "Topic 목록 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회성공"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @GetMapping("/topics")
    public ResponseEntity<BaseResponse<List<Item>>> getTopics() {
        List<Item> topics = service.getTopics();
        return ResponseEntity.ok(BaseResponse.success(topics));
    }

    @Operation(summary = "Topic 상세정보 조회(메타데이터)", description = "Topic 상세정보 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회성공"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @GetMapping("/topics/{topicId}")
    public ResponseEntity<BaseResponse<Item>> getTopicById(@PathVariable Long topicId) {
        return ResponseEntity.ok(BaseResponse.success(service.getTopicById(topicId)));
    }

    @Operation(summary = "Topic Schema 업데이트", description = "Topic의 Schema를 업데이트 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "update 성공"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
    })
    @PutMapping("/topics/{topicId}/schema")
    public ResponseEntity<BaseResponse<Integer>> updateTopicSchema(
        @PathVariable Long topicId,
        @RequestBody @Valid TopicRequest.Schema schema) {

        int updated = service.updateTopicSchema(topicId, schema);
        return ResponseEntity.ok(BaseResponse.success("schema updated", updated));
    }

    @Operation(summary = "Topic의 권한정보 조회", description = "Topic의 권한정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회성공"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @GetMapping("/topics/{topicId}/permissions")
    public ResponseEntity<BaseResponse<?>> getPermissionsOfTopic(
        @PathVariable Long topicId
    ) {
        return ResponseEntity.ok(BaseResponse.of());
    }

    @Operation(summary = "Topic의 권한정보를 등록/수정", description = "Topic의 권한정보를 등록/수정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "처리성공"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @PostMapping("/topics/{topicId}/permissions")
    public ResponseEntity<BaseResponse<?>> postTopicPermissions(
        @PathVariable Long topicId
    ) {
        return ResponseEntity.ok(BaseResponse.of());
    }

    @Operation(summary = "특정 사용자의 Topic의 권한정보를 조회", description = "특정 사용자의 Topic의 권한정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회성공"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @GetMapping("/topics/{topicId}/permissions")
    public ResponseEntity<BaseResponse<?>> getPermissionsByUserId(
        @RequestParam Long userId
    ) {
        return ResponseEntity.ok(BaseResponse.of());
    }

}