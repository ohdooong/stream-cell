package com.streamcell.platform.pipeline.dto;

import com.streamcell.platform.flink.enums.FlinkJobStatus;
import com.streamcell.platform.pipeline.enums.ArtifactType;
import com.streamcell.platform.pipeline.enums.DeploymentStatus;
import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.pipeline.enums.PipelineType;
import java.time.LocalDateTime;
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
        private com.streamcell.platform.pipeline.enums.PipelineStatus pipelineStatus;
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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class Deployment {
        private Long pipelineId;
        private Long deploymentId;
        private String flinkJarId;
        private String flinkJobId;
        private DeploymentStatus status;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class PipelineStatus {
        private Long pipelineId;
        private Long deploymentId;
        private String flinkJobId;
        private FlinkJobStatus flinkJobStatus;
        private com.streamcell.platform.pipeline.enums.PipelineStatus pipelineStatus;
        private DeploymentStatus deploymentStatus;
        private Failure failure;

        @AllArgsConstructor(staticName = "from")
        public static class Failure {
            private String errorExceptionName;
            private String errorMessage;
            private Long errorTimestamp;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class StopPipeline {
        private Long pipelineId;
        private Long deploymentId;
        private String flinkJobId;
        private com.streamcell.platform.pipeline.enums.PipelineStatus pipelineStatus;
    }

}

