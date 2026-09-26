package com.streamcell.platform.pipeline.service;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.flink.client.FlinkRestClient;
import com.streamcell.platform.flink.enums.FlinkJobStatus;
import com.streamcell.platform.pipeline.converter.PipelineDeploymentConverter;
import com.streamcell.platform.pipeline.domain.DeploymentStatusPolicy;
import com.streamcell.platform.pipeline.domain.JobStatusConvertPolicy;
import com.streamcell.platform.pipeline.dto.PipelineDeploymentRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.dto.PipelineResponse.Deployment;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineDeployment;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface PipelineDeploymentService {

    /**
     * 등록된 custom jar를 flink cluster에 배포합니다. {@link com.streamcell.platform.pipeline.service.impl.PipelineDeploymentCustomJarServiceImpl PipelineDeploymentCustomJarServiceImpl}
     * 등록된 AI SQL정보를 바탕으로 배포한다. {@link com.streamcell.platform.pipeline.service.impl.PipelineDeploymentAISqlServiceImpl PipelineDeploymentAISqlServiceImpl}
     *
     * @param pipelineId pipeline PK
     * @return 배포된 pipeline deployment
     */
    PipelineResponse.Deployment deploy(Long pipelineId);

    default List<PipelineResponse.Deployment> findByPipelineId(Long pipelineId) {
        return getPipelineRepository().findPipelineDeploymentByPipelineId(pipelineId)
                .stream()
                .map(getPipelineDeploymentConverter()::toDto)
                .toList();
    };

    default PipelineResponse.Deployment createPipelineDeployment(PipelineDeploymentRequest.Create create) {

        PipelineDeployment pipelineDeployment = getPipelineDeploymentConverter().toVo(create);

        getPipelineRepository().insertPipelineDeployment(pipelineDeployment);

        return Deployment.builder()
                .deploymentId(pipelineDeployment.getDeploymentId())
                .pipelineId(pipelineDeployment.getPipelineId())
                .flinkJobId(pipelineDeployment.getFlinkJobId())
                .status(pipelineDeployment.getStatus()).build();
    };

    /**
     * pipeline의 Flink Job을 Cancel 시킴.
     *
     * @param pipelineId pipeline PK
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    default PipelineResponse.StopPipeline cancelPipelineFlinkJob(Long pipelineId) {

        Pipeline pipeline = getPipelineRepository().findPipelineByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));

        // todo 사용자 권한/소유자 검증
        PipelineDeployment pipelineDeployment = getPipelineRepository().findLatestPipelineDeployMentByPipelineId(pipelineId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE_DEPLOYMENT));

        // 중지 가능한 상태인지 검증
        boolean availableStop = getDeploymentStatusPolicy().isAvailableStop(pipelineDeployment.getStatus());
        if (!availableStop) {
            throw new BaseAPIException(ErrorCode.INVALID_CANCEL_FLINK_JOB);
        }

        // cancel job 호출
        FlinkJobStatus flinkJobStatus = getFlinkRestClient().cancelJob(pipelineDeployment.getFlinkJobId());

        // pipeline STOPPING update
        PipelineStatus pipelineStatus = getJobStatusConvertPolicy().convertToPipelineStatusFrom(flinkJobStatus);
        pipeline.setPipelineStatus(pipelineStatus);
        getPipelineRepository().updatePipelineStatus(pipeline);

        // pipeline deployment STOPPING update
        DeploymentStatus deploymentStatus = getJobStatusConvertPolicy().convertToDeploymentStatusFrom(flinkJobStatus);
        pipelineDeployment.setStatus(deploymentStatus);
        pipelineDeployment.setStoppedAt(LocalDateTime.now());
        getPipelineRepository().updatePipelineDeploymentStatus(pipelineDeployment);

        return PipelineResponse.StopPipeline.builder()
                .pipelineId(pipelineId)
                .deploymentId(pipelineDeployment.getDeploymentId())
                .flinkJobId(pipelineDeployment.getFlinkJobId())
                .pipelineStatus(PipelineStatus.STOPPING)
                .build();
    };

    PipelineRepository getPipelineRepository();
    PipelineDeploymentConverter getPipelineDeploymentConverter();
    FlinkRestClient getFlinkRestClient();
    JobStatusConvertPolicy getJobStatusConvertPolicy();
    DeploymentStatusPolicy getDeploymentStatusPolicy();
}
