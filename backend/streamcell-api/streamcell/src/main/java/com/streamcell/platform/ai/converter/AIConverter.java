package com.streamcell.platform.ai.converter;

import com.streamcell.platform.ai.domain.context.FlinkSQLGenerationContext;
import com.streamcell.platform.ai.domain.context.KafkaSourceDDLGenerationContext;
import com.streamcell.platform.ai.domain.context.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AIConverter {

    FlinkSQLGenerationContext toGenerationContext(PipelinePlanValidationContext validationContext);

    KafkaSourceDDLGenerationContext toKafkaSourceDDLGenerationContext(PipelinePlanValidationContext validationContext);

    PostgreSQLSinkDDLGenerationContext toPostgreSQLGenerationContext(PipelinePlanValidationContext validationContext);
}