package com.streamcell.platform.pipeline.service;

import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PipelineService {

    PipelineResponse.Pipeline create(PipelineRequest.Create createItem);

    PipelineResponse.Pipeline update(PipelineRequest.Update updateItem);

    PipelineResponse.Pipeline findPipelineByPipelineId(Long pipelineId);

    PipelineResponse.Artifact createFlinkCustomJar(MultipartFile file, PipelineRequest.CreateCustomJobConfig createCustomJobConfig, Long pipelineId);

}
