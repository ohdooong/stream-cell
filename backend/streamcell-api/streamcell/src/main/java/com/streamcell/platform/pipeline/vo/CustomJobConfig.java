package com.streamcell.platform.pipeline.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class CustomJobConfig {
    private Long configId;
    private Long pipelineId;
    private String entryClass;
    private List<Long> inputTopicIds;
    private List<Long> outputTopicIds;
    private Integer parallelism;
    private Map<String, Object> programArgs;
}
