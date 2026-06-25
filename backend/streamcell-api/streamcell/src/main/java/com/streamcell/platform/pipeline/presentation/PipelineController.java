package com.streamcell.platform.pipeline.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Platform Pipeline API", description = "Pipeline(파이프라인관리) API 컨트롤러")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/platform/pipeline")
public class PipelineController {

    private final PipelineService service;

    @Operation(summary = "Pipeline 생성", description = "Pipeline을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @PostMapping("/pipelines")
    public ResponseEntity<BaseResponse<?>> createPipeline(
            @RequestBody @Valid PipelineRequest.Create createItem) {
        return ResponseEntity.ok(
                BaseResponse.success(service.create(createItem)));
    }

    @Operation(summary = "Pipeline 수정", description = "Pipeline을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @PatchMapping("/pipelines")
    public ResponseEntity<BaseResponse<?>> updatePipeline(
            @RequestBody @Valid PipelineRequest.Update updateItem) {
        return ResponseEntity.ok(
                BaseResponse.success(service.update(updateItem)));
    }

    @Operation(summary = "Pipeline 상세조회", description = "Pipeline 정보를 상세조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @GetMapping("/pipelines/{pipelineId}")
    public ResponseEntity<BaseResponse<?>> getPipelineByPipelineId(
            @PathVariable Long pipelineId) {
        return ResponseEntity.ok(
                BaseResponse.success(service.findPipelineByPipelineId(pipelineId)));
    }

}
