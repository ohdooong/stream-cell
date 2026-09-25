package com.streamcell.platform.pipeline.converter;

import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.vo.CustomJobConfig;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PipelineConverter {

    PipelineResponse.Pipeline toDTO(Pipeline vo);

    default Pipeline toVO(PipelineRequest.Create createItem) {
        return Pipeline.builder()
                .ownerUserId(createItem.getOwnerUserId())
                .pipelineName(createItem.getPipelineName())
                .description(createItem.getDescription())
                .pipelineType(createItem.getPipelineType())
                .pipelineStatus(PipelineStatus.CREATED)
                .build();
    }

    Pipeline toVO(PipelineRequest.Update updateItem);


    PipelineResponse.Artifact toDTO(PipelineArtifact artifact);

    default CustomJobConfig toVO(PipelineRequest.CreateCustomJobConfig createCustomJobConfig, Long pipelineId) {
        return CustomJobConfig.builder()
                .userId(createCustomJobConfig.getUserId())
                .pipelineId(pipelineId)
                .entryClass(createCustomJobConfig.getEntryClass())
                .inputTopicIds(createCustomJobConfig.getInputTopicIds())
                .outputTopicIds(createCustomJobConfig.getOutputTopicIds())
                .parallelism(createCustomJobConfig.getParallelism())
                .programArgs(createCustomJobConfig.getProgramArgs()).build();
    }


}
