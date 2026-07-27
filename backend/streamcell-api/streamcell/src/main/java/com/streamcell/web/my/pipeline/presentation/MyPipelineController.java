package com.streamcell.web.my.pipeline.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.web.my.pipeline.service.MyPipelinesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "My Pipeline API", description = "My Pipeline(나의 파이프라인) API 컨트롤러")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/web/my/pipeline")
public class MyPipelineController {

    private final MyPipelinesService service;


    @Operation(summary = "나의 Pipeline목록 조회", description = "내가 배포한 Pipeline들의 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @GetMapping("/pipelines")
    public ResponseEntity<BaseResponse<?>> getPipelinesByUserId(
            @RequestParam Long userId) {
        return ResponseEntity.ok(
                BaseResponse.success(service.findPipelinesByUserId(userId)));
    }
}
