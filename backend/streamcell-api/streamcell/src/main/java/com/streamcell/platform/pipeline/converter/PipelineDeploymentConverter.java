package com.streamcell.platform.pipeline.converter;

import com.streamcell.platform.pipeline.dto.PipelineDeploymentRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.vo.PipelineDeployment;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PipelineDeploymentConverter {

    PipelineDeployment toVo(PipelineDeploymentRequest.Create create);

    PipelineResponse.Deployment toDto(PipelineDeployment pipelineDeployment);
}
