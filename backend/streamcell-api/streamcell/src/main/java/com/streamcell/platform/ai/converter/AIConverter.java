package com.streamcell.platform.ai.converter;

import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.pipeline.vo.Pipeline;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AIConverter {

    Pipeline toPipelineVO(PipelinePlanValidationContext context);


}