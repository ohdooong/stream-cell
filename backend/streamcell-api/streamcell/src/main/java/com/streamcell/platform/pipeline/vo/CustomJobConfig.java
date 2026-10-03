package com.streamcell.platform.pipeline.vo;

import com.streamcell.global._common.vo.BaseVO;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomJobConfig extends BaseVO {
    private Long userId;
    private Long configId;
    private Long pipelineId;
    private String entryClass;
    private List<Long> inputTopicIds;
    private List<Long> outputTopicIds;
    private Integer parallelism;
    private ProgramArgs programArgs;
}
