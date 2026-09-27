package com.streamcell.platform.pipeline.vo;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISqlConfig {
    private Long aiConfigId;
    private Long pipelineId;
    private Long inputTopicId;
}
