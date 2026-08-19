package com.streamcell.platform.pipeline.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.service.PipelineDeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Platform Pipeline Deployment API", description = "Pipeline Deployment 관리 API 컨트롤러")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/platform/pipeline/pipelines/deployment")
public class PipelineDeploymentController {
    private final PipelineDeploymentService service;

    @Operation(summary = "Flink Jar 배포", description = "등록한 Custom Jar를 Flink로 배포합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flink 배포 성공"),
            @ApiResponse(responseCode = "400", description = "Flink 배포 실패"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @PostMapping(value = "/{pipelineId}/deploy")
    public ResponseEntity<BaseResponse<PipelineResponse.Deployment>> deployCustomJarToFlink(@PathVariable Long pipelineId) {
        PipelineResponse.Deployment result = service.deploy(pipelineId);

        if (DeploymentStatus.FAILED == result.getStatus()) {
            return ResponseEntity.badRequest().body(
                    BaseResponse.error(ErrorCode.FAILED_FLINK_DEPLOY, result));
        }

        return ResponseEntity.ok(BaseResponse.success(result));
    }


    @Operation(summary = "Flink Job 중지", description = "배포한 Flink Job을 중지합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Job Cancel 성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @PostMapping("/{pipelineId}/stop")
    public ResponseEntity<BaseResponse<?>> cancelPipelineFlinkJob(
            @PathVariable Long pipelineId
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(service.cancelPipelineFlinkJob(pipelineId)));
    }
}
