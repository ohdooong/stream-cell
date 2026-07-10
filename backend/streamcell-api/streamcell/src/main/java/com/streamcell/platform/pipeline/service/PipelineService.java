package com.streamcell.platform.pipeline.service;

import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PipelineService {

    /**
     * 파이프라인 create
     * @param createItem
     * @return pipeline response dto.
     */
    PipelineResponse.Pipeline create(PipelineRequest.Create createItem);

    /**
     * 파이프라인 update
     * @param updateItem
     * @return pipeline response dto.
     */
    PipelineResponse.Pipeline update(PipelineRequest.Update updateItem);

    /**
     * 파이프라인id로 파이프라인 찾기
     * @param pipelineId
     * @return pipeline response dto.
     */
    PipelineResponse.Pipeline findPipelineByPipelineId(Long pipelineId);

    /**
     * customJar 업로드
     * @param file
     * @param createCustomJobConfig
     * @param pipelineId
     * @return
     */
    PipelineResponse.Artifact createFlinkCustomJar(MultipartFile file, PipelineRequest.CreateCustomJobConfig createCustomJobConfig, Long pipelineId);

}
