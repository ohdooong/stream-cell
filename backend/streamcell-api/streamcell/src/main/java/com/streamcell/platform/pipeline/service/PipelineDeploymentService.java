package com.streamcell.platform.pipeline.service;

import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.dto.PipelineResponse.Deployment;
import java.util.List;

public interface PipelineDeploymentService {

    /**
     * 등록된 custom jar를 flink cluster에 배포합니다.
     *
     * @param pipelinId
     */
    PipelineResponse.Deployment deploy(Long pipelinId);

    List<Deployment> findByPipelineId(Long pipelineId);
}
