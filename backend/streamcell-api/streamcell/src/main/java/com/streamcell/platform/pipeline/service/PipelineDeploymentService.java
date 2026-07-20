package com.streamcell.platform.pipeline.service;

public interface PipelineDeploymentService {

    /**
     * 등록된 custom jar를 flink cluster에 배포합니다.
     *
     * @param pipelinId
     */
    void deploy(Long pipelinId);
}
