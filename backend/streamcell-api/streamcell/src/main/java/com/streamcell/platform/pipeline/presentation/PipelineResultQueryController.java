package com.streamcell.platform.pipeline.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.platform.pipeline.enums.PipelineType;
import com.streamcell.platform.pipeline.service.PipelineResultQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Pipeline Result API", description = "Pipeline Result(파이프라인 실시간 결과) API 컨트롤러")
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
    @GetMapping("/pipelines/{pipelineId}/results")
    public ResponseEntity<BaseResponse<?>> getPipelineResults(
            @PathVariable Long pipelineId,
            @RequestParam @Valid @NotNull Integer limit,
            @RequestParam @Valid @NotNull Integer rangeMinutes
    ) {
        return ResponseEntity.ok(BaseResponse.success(service.getPipelineResults(pipelineId, limit, rangeMinutes)));
    }

}
