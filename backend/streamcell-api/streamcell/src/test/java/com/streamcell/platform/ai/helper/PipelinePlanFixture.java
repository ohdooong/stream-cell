package com.streamcell.platform.ai.helper;

import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.domain.spec.FilterSpec;
import com.streamcell.platform.ai.domain.spec.WindowSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.enums.AggregationFunction;
import com.streamcell.platform.ai.enums.FilterOperator;
import com.streamcell.platform.ai.enums.WindowType;
import com.streamcell.platform.ai.enums.WindowUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PipelinePlanFixture {

    private PipelinePlanFixture() {
    }

    /**
     * 기본 정상 PipelinePlan
     *
     * 5분마다 상품별
     * - 주문 건수
     * - 평균 결제금액
     * 집계
     *
     * payment_amount >= 10000
     */
    public static PipelinePlan validPipelinePlan() {
        PipelinePlan plan = new PipelinePlan();

        plan.setSourceTopicId(1L);
        plan.setWindow(validWindow());

        plan.setGroupBy(
            List.of("product_id")
        );

        plan.setAggregations(
            List.of(
                countAll("order_count"),
                avg("payment_amount", "avg_payment_amount")
            )
        );

        plan.setFilters(
            List.of(
                filter(
                    "payment_amount",
                    FilterOperator.GTE,
                    10000
                )
            )
        );

        return plan;
    }

    public static WindowSpec validWindow() {
        WindowSpec window = new WindowSpec();

        window.setType(WindowType.TUMBLE);
        window.setSize(5);
        window.setUnit(WindowUnit.MINUTE);

        return window;
    }

    public static AggregationSpec countAll(String alias) {
        return aggregation(
            AggregationFunction.COUNT,
            "*",
            alias
        );
    }

    public static AggregationSpec count(
        String field,
        String alias
    ) {
        return aggregation(
            AggregationFunction.COUNT,
            field,
            alias
        );
    }

    public static AggregationSpec sum(
        String field,
        String alias
    ) {
        return aggregation(
            AggregationFunction.SUM,
            field,
            alias
        );
    }

    public static AggregationSpec avg(
        String field,
        String alias
    ) {
        return aggregation(
            AggregationFunction.AVG,
            field,
            alias
        );
    }

    public static AggregationSpec min(
        String field,
        String alias
    ) {
        return aggregation(
            AggregationFunction.MIN,
            field,
            alias
        );
    }

    public static AggregationSpec max(
        String field,
        String alias
    ) {
        return aggregation(
            AggregationFunction.MAX,
            field,
            alias
        );
    }

    public static AggregationSpec aggregation(
        AggregationFunction function,
        String field,
        String alias
    ) {
        AggregationSpec aggregation = new AggregationSpec();

        aggregation.setFunction(function);
        aggregation.setField(field);
        aggregation.setAlias(alias);

        return aggregation;
    }

    public static FilterSpec filter(
        String field,
        FilterOperator operator,
        Object value
    ) {
        FilterSpec filter = new FilterSpec();

        filter.setField(field);
        filter.setOperator(operator);
        filter.setValue(value);

        return filter;
    }

    /**
     * 테스트에서 공통으로 사용할 Topic Schema
     */
    public static Map<String, Object> validTopicSchema() {
        Map<String, Object> schema = new HashMap<>();

        schema.put("order_id", "STRING");
        schema.put("product_id", "STRING");
        schema.put("payment_amount", "DECIMAL(18,2)");
        schema.put("quantity", "INT");
        schema.put("event_time", "TIMESTAMP");

        return schema;
    }

    /**
     * Schema/Window/Aggregation/Filter Validator에서
     * 바로 사용할 수 있는 기본 Context
     */
    public static PipelinePlanValidationContext createContext() {

        PipelinePlanValidationContext context =
            new PipelinePlanValidationContext();

        context.setPipelinePlan(validPipelinePlan());
        context.setParsedTopicSchema(validTopicSchema());

        return context;
    }
}