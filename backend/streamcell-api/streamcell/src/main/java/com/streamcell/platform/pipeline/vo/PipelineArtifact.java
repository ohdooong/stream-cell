package com.streamcell.platform.pipeline.vo;

import com.streamcell.platform.pipeline.enums.ArtifactType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineArtifact {
    private Long artifactId;
    private Long pipelineId;
    private ArtifactType artifactType;
    private String originalFileName;
    private String storedFileName;
    private String storedFilePath;
    private String flinkJarId;
}
