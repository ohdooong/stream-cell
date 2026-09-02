package com.streamcell.platform.ai.domain;

import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.domain.spec.FilterSpec;
import com.streamcell.platform.ai.domain.spec.WindowSpec;
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

    public void validate() {

    }
}
