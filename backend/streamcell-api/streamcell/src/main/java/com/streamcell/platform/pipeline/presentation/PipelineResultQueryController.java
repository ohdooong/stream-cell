package com.streamcell.platform.pipeline.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.platform.pipeline.service.PipelineResultQueryService;
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
@Tag(name = "Platform Pipeline API", description = "Pipeline(파이프라인관리) API 컨트롤러")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/platform/pipeline")
public class PipelineResultQueryController {

    private final PipelineResultQueryService service;

    @Operation(summary = "Pipeline Result 대시보드 조회", description = "Pipeline의 Result값을 대시보드로 조회하기 위해 차트값들을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @GetMapping("/pipelines")
    public ResponseEntity<BaseResponse<?>> getPipelineResultValue() {
        return ResponseEntity.ok(BaseResponse.success(null));
    }

}
