package com.streamcell.platform.pipeline.dto;

import com.streamcell.platform.pipeline.enums.ArtifactType;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import lombok.*;

import java.util.List;
import java.util.Map;

public class PipelineResponse {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "from")
    public static class Pipeline {
        private Long pipelineId;
        private Long ownerUserId;
        private String pipelineName;
        private String description;
        private PipelineType pipelineType;
        private PipelineStatus pipelineStatus;
        private String naturalLanguageRequest;
        private String pipelinePlanJson;
        private String generatedSql;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class Artifact {
        private Long artifactId;
        private Long pipelineId;
        private ArtifactType artifactType;
        private String originalFileName;
        private String storedFileName;
        private String storedFilePath;
        private String flinkJarId;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class CustomJobConfig {
        private Long configId;
        private Long pipelineId;
        private String entryClass;
        private List<Long> inputTopicIds;
        private List<Long> outputTopicIds;
        private Integer parallelism;
        private Map<String, Object> programArgs;
    }


}

