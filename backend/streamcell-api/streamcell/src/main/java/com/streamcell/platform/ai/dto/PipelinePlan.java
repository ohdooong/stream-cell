package com.streamcell.platform.ai.dto;

import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.domain.spec.FilterSpec;
import com.streamcell.platform.ai.domain.spec.WindowSpec;
import com.streamcell.platform.ai.domain.validator.CompositeValidator;
import com.streamcell.platform.ai.domain.validator.PipelineValidator;
import com.streamcell.platform.ai.domain.validator.TopicValidator;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.topic.vo.Topic;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

/**
 * Pipeline plan.
 * AI가 생성한 Pipeline Plan
 * JSON을 Pipeline Plan 객체로 변환
 *
 * @see WindowSpec
 * @see AggregationSpec
 * @see FilterSpec
 *
 */
@ToString
@Getter
public class PipelinePlan {

    private Long sourceTopicId;

    private WindowSpec window;

    private List<String> groupBy;

    private List<AggregationSpec> aggregations;

    private List<FilterSpec> filters;

}
