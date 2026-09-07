package com.streamcell.platform.ai.converter;

import com.streamcell.platform.ai.domain.context.PipelinePlanValidationContext;
import com.streamcell.platform.pipeline.vo.Pipeline;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-04T22:10:56+0900",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.5 (Oracle Corporation)"
)
@Component
public class AIConverterImpl implements AIConverter {

    @Override
    public Pipeline toPipelineVO(PipelinePlanValidationContext context) {
        if ( context == null ) {
            return null;
        }

        Pipeline.PipelineBuilder pipeline = Pipeline.builder();

        return pipeline.build();
    }
}
