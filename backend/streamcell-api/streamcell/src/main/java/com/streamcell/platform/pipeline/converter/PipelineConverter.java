package com.streamcell.platform.pipeline.converter;

import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.enums.ArtifactType;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.vo.CustomJobConfig;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;


public class PipelineConverter {

    public static PipelineResponse.Pipeline toDTO(Pipeline vo) {
        return PipelineResponse.Pipeline.builder()
                .pipelineId(vo.getPipelineId())
                .ownerUserId(vo.getOwnerUserId())
                .pipelineName(vo.getPipelineName())
                .description(vo.getDescription())
                .pipelineType(vo.getPipelineType())
                .pipelineStatus(vo.getPipelineStatus())
                .naturalLanguageRequest(vo.getNaturalLanguageRequest())
                .pipelinePlanJson(vo.getPipelinePlanJson())
                .generatedSql(vo.getGeneratedSql())
                .build();
    }

    public static Pipeline toVO(PipelineRequest.Create createItem) {
        return Pipeline.builder()
                .ownerUserId(createItem.getOwnerUserId())
                .pipelineName(createItem.getPipelineName())
                .description(createItem.getDescription())
                .pipelineType(createItem.getPipelineType())
                .pipelineStatus(PipelineStatus.CREATED)
                .build();
    }

    public static Pipeline toVO(PipelineRequest.Update updateItem) {
        return Pipeline.builder()
                .pipelineId(updateItem.getPipelineId())
                .ownerUserId(updateItem.getOwnerUserId())
                .pipelineName(updateItem.getPipelineName())
                .description(updateItem.getDescription())
                .pipelineType(updateItem.getPipelineType())
                .build();
    }


    public static PipelineResponse.Artifact toDTO(PipelineArtifact artifact) {
        return PipelineResponse.Artifact.builder()
                .artifactId(artifact.getArtifactId())
                .pipelineId(artifact.getPipelineId())
                .artifactType(artifact.getArtifactType())
                .originalFileName(artifact.getOriginalFileName())
                .storedFileName(artifact.getStoredFileName())
                .storedFilePath(artifact.getStoredFilePath())
                .flinkJarId(artifact.getFlinkJarId())
                .build();
    }

    public static CustomJobConfig toVO(PipelineRequest.CreateCustomJobConfig createCustomJobConfig, Long pipelineId) {
        return CustomJobConfig.builder()
                .pipelineId(pipelineId)
                .entryClass(createCustomJobConfig.getEntryClass())
                .inputTopicIds(createCustomJobConfig.getInputTopicIds())
                .outputTopicIds(createCustomJobConfig.getOutputTopicIds())
                .parallelism(createCustomJobConfig.getParallelism())
                .programArgs(createCustomJobConfig.getProgramArgs()).build();
    }
}
