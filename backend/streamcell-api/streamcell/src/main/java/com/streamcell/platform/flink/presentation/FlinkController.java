package com.streamcell.platform.flink.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.flink.dto.FlinkResponse;
import com.streamcell.platform.flink.service.FlinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Flink API", description = "Flink API 컨트롤러")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/platform/flink")
public class FlinkController {

    private final FlinkService service;

    @Operation(summary = "Flink Cluster Info 조회", description = "Flink Cluster의 상세정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @GetMapping("/cluster-overview")
    public ResponseEntity<BaseResponse<FlinkResponse.ClusterOverview>> getClusterOverview() {
        FlinkResponse.ClusterOverview response = service.getClusterOverview();
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
