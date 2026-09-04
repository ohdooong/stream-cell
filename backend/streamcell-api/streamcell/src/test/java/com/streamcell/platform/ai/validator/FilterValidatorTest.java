package com.streamcell.platform.ai.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.PipelinePlanValidationContext;
import com.streamcell.platform.ai.domain.spec.FilterSpec;
import com.streamcell.platform.ai.domain.validator.FilterValidator;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.enums.FilterOperator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class FilterValidatorTest {

    private FilterValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FilterValidator();
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("Numeric Field에 Numeric Value와 GTE를 사용할 수 있다")
        void numericFieldWithNumericValue() {

            PipelinePlanValidationContext context = createContext(
                Map.of(
                    "payment_amount", "DECIMAL(18,2)",
                    "product_id", "STRING"
                ),
                List.of(
                    createFilter(
                        "payment_amount",
                        FilterOperator.GTE,
                        10000
                    )
                )
            );

            assertDoesNotThrow(() ->
                validator.validate(context)
            );
        }

        @Test
        @DisplayName("Non-Numeric Field에는 EQ를 사용할 수 있다")
        void nonNumericFieldWithEq() {

            PipelinePlanValidationContext context = createContext(
                Map.of(
                    "product_id", "STRING"
                ),
                List.of(
                    createFilter(
                        "product_id",
                        FilterOperator.EQ,
                        "P001"
                    )
                )
            );

            assertDoesNotThrow(() ->
                validator.validate(context)
            );
        }

        @Test
        @DisplayName("Numeric Field에는 EQ도 사용할 수 있다")
        void numericFieldWithEq() {

            PipelinePlanValidationContext context = createContext(
                Map.of(
                    "payment_amount", "DECIMAL"
                ),
                List.of(
                    createFilter(
                        "payment_amount",
                        FilterOperator.EQ,
                        10000
                    )
                )
            );

            assertDoesNotThrow(() ->
                validator.validate(context)
            );
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCases {

        @Test
        @DisplayName("Numeric Field에 String Value를 사용할 수 없다")
        void numericFieldWithStringValue() {

            PipelinePlanValidationContext context = createContext(
                Map.of(
                    "payment_amount", "DECIMAL"
                ),
                List.of(
                    createFilter(
                        "payment_amount",
                        FilterOperator.EQ,
                        "10000"
                    )
                )
            );

            assertThrows(
                BaseAPIException.class,
                () -> validator.validate(context)
            );
        }

        @Test
        @DisplayName("Non-Numeric Field에는 GTE를 사용할 수 없다")
        void nonNumericFieldWithGte() {

            PipelinePlanValidationContext context = createContext(
                Map.of(
                    "product_id", "STRING"
                ),
                List.of(
                    createFilter(
                        "product_id",
                        FilterOperator.GTE,
                        "P001"
                    )
                )
            );

            assertThrows(
                BaseAPIException.class,
                () -> validator.validate(context)
            );
        }

        @Test
        @DisplayName("Non-Numeric Field에 Number Value를 사용할 수 없다")
        void nonNumericFieldWithNumberValue() {

            PipelinePlanValidationContext context = createContext(
                Map.of(
                    "product_id", "STRING"
                ),
                List.of(
                    createFilter(
                        "product_id",
                        FilterOperator.EQ,
                        100
                    )
                )
            );

            assertThrows(
                BaseAPIException.class,
                () -> validator.validate(context)
            );
        }
    }

    private PipelinePlanValidationContext createContext(
        Map<String, Object> schema,
        List<FilterSpec> filters
    ) {
        PipelinePlan pipelinePlan = new PipelinePlan();
        pipelinePlan.setFilters(filters);

        PipelinePlanValidationContext context =
            new PipelinePlanValidationContext();

        context.setPipelinePlan(pipelinePlan);
        context.setParsedTopicSchema(schema);

        return context;
    }

    private FilterSpec createFilter(
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
}
