package com.streamcell.platform.pipeline.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.platform.pipeline.service.PipelineDeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Platform Pipeline Deployment API", description = "Pipeline Deployment 관리 API 컨트롤러")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/platform/pipeline/pipelines/deployment")
public class PipelineDeploymentController {
    private final PipelineDeploymentService service;


    @Operation(summary = "Flink Jar 배포", description = "등록한 Custom Jar를 Flink로 배포합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배포 성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @PostMapping(value = "/{pipelineId}/deploy")
    public ResponseEntity<BaseResponse<?>> deployCustomJarToFlink(@PathVariable Long pipelineId) {

        service.deploy(pipelineId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
