package com.streamcell.platform.ai.domain.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import com.streamcell.platform.ai.domain.resolver.AggregationTypeResolver;
import com.streamcell.platform.ai.domain.resolver.PostgreSQLTypeResolver;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.dto.PipelineResultTable;
import com.streamcell.platform.ai.repository.PipelineResultRepository;
import com.streamcell.platform.pipeline.vo.Pipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PipelineResultTableManagerTest {

    private static final JsonMapper jsonMapper = new JsonMapper();

    private PipelineResultRepository repository;

    private PipelineResultTableManager manager;

    @BeforeEach
    void setUp() {
        repository = mock(PipelineResultRepository.class);

        PostgreSQLTypeResolver postgreSQLTypeResolver =
                new PostgreSQLTypeResolver();

        AggregationTypeResolver aggregationTypeResolver =
                new AggregationTypeResolver();

        manager = new PipelineResultTableManager(
                postgreSQLTypeResolver,
                aggregationTypeResolver,
                repository
        );
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("PipelinePlan 기반으로 PostgreSQL Result Table 정보를 생성한다")
        void createPipelineResultTable() throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context =
                    createContext("DOUBLE");

            // when
            manager.createTable(context);

            // then
            ArgumentCaptor<PipelineResultTable> captor =
                    ArgumentCaptor.forClass(PipelineResultTable.class);

            verify(repository, times(1))
                    .createPipelineResultTable(captor.capture());

            PipelineResultTable result = captor.getValue();

            assertEquals(
                    "pipeline_result_1",
                    result.getTableName()
            );

            String expectedColumns = """
                    window_start TIMESTAMP(3),
                        window_end TIMESTAMP(3),
                        product_id VARCHAR,
                        order_count BIGINT,
                        avg_payment_amount DOUBLE PRECISION
                    """.strip();

            assertEquals(
                    expectedColumns,
                    result.getColumns()
            );
        }

        @Test
        @DisplayName("COUNT(*) 결과 컬럼은 BIGINT 타입으로 생성한다")
        void countResultTypeIsBigint() throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context =
                    createContext("DOUBLE");

            // when
            manager.createTable(context);

            // then
            ArgumentCaptor<PipelineResultTable> captor =
                    ArgumentCaptor.forClass(PipelineResultTable.class);

            verify(repository)
                    .createPipelineResultTable(captor.capture());

            PipelineResultTable result = captor.getValue();

            org.junit.jupiter.api.Assertions.assertTrue(
                    result.getColumns()
                            .contains("order_count BIGINT")
            );
        }

        @Test
        @DisplayName("Flink DOUBLE 타입은 PostgreSQL DOUBLE PRECISION으로 변환한다")
        void doubleTypeConvertedToDoublePrecision()
                throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context =
                    createContext("DOUBLE");

            // when
            manager.createTable(context);

            // then
            ArgumentCaptor<PipelineResultTable> captor =
                    ArgumentCaptor.forClass(PipelineResultTable.class);

            verify(repository)
                    .createPipelineResultTable(captor.capture());

            PipelineResultTable result = captor.getValue();

            org.junit.jupiter.api.Assertions.assertTrue(
                    result.getColumns()
                            .contains(
                                    "avg_payment_amount DOUBLE PRECISION"
                            )
            );
        }

        @Test
        @DisplayName("GroupBy 컬럼은 PostgreSQL 타입으로 변환한다")
        void groupByColumnConvertedToPostgreSQLType()
                throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context =
                    createContext("DOUBLE");

            // when
            manager.createTable(context);

            // then
            ArgumentCaptor<PipelineResultTable> captor =
                    ArgumentCaptor.forClass(PipelineResultTable.class);

            verify(repository)
                    .createPipelineResultTable(captor.capture());

            PipelineResultTable result = captor.getValue();

            org.junit.jupiter.api.Assertions.assertTrue(
                    result.getColumns()
                            .contains("product_id VARCHAR")
            );
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCases {

        @Test
        @DisplayName("현재 MVP에서 AVG(DECIMAL)은 Result Table을 생성하지 않는다")
        void avgDecimalFail() throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context =
                    createContext("DECIMAL(18,2)");

            // when & then
            assertThrows(
                    BaseAPIException.class,
                    () -> manager.createTable(context)
            );

            verify(
                    repository,
                    never()
            ).createPipelineResultTable(any());
        }
    }

    private PostgreSQLSinkDDLGenerationContext createContext(
            String paymentAmountType
    ) throws JsonProcessingException {

        return PostgreSQLSinkDDLGenerationContext.builder()
                .userId(1L)
                .pipeline(createPipeline())
                .pipelinePlan(createPipelinePlan())
                .parsedTopicSchema(
                        createParsedTopicSchema(
                                paymentAmountType
                        )
                )
                .build();
    }

    private Pipeline createPipeline() {

        Pipeline pipeline = mock(Pipeline.class);

        when(pipeline.getPipelineId())
                .thenReturn(1L);

        return pipeline;
    }

    private Map<String, Object> createParsedTopicSchema(
            String paymentAmountType
    ) {

        Map<String, Object> schema =
                new LinkedHashMap<>();

        schema.put("order_id", "STRING");
        schema.put("product_id", "STRING");
        schema.put(
                "payment_amount",
                paymentAmountType
        );
        schema.put("quantity", "INT");
        schema.put(
                "event_time",
                "TIMESTAMP(3)"
        );

        return schema;
    }

    private PipelinePlan createPipelinePlan()
            throws JsonProcessingException {

        String sourceJson = """
                {
                  "sourceTopicId": 1,
                  "window": {
                    "type": "TUMBLE",
                    "size": 5,
                    "unit": "MINUTE"
                  },
                  "groupBy": [
                    "product_id"
                  ],
                  "aggregations": [
                    {
                      "function": "COUNT",
                      "field": "*",
                      "alias": "order_count"
                    },
                    {
                      "function": "AVG",
                      "field": "payment_amount",
                      "alias": "avg_payment_amount"
                    }
                  ],
                  "filters": []
                }
                """;

        return jsonMapper.readValue(
                sourceJson,
                PipelinePlan.class
        );
    }
}