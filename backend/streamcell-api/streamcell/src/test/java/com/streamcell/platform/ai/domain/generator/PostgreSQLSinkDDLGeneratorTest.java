package com.streamcell.platform.ai.domain.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.global.config.DBConfig;
import com.streamcell.platform.ai.domain.context.PostgreSQLSinkDDLGenerationContext;
import com.streamcell.platform.ai.domain.policy.FlinkSQLPolicy;
import com.streamcell.platform.ai.domain.policy.PostgreSQLSinkPolicy;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.vo.Pipeline;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.config.location=classpath:test-application.yaml")
class PostgreSQLSinkDDLGeneratorTest {

    private static final JsonMapper jsonMapper = new JsonMapper();

    @Autowired
    private PostgreSQLSinkDDLGenerator generator;

    @Autowired
    private DBConfig dbConfig;

    @Nested
    @DisplayName("정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("GroupBy, COUNT, AVG를 포함한 PostgreSQL Sink DDL을 생성한다")
        void generateSinkDDL() throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context = createContext();

            // when
            String actual = generator.generate(context);

            // then
            String expected = """
                    CREATE TEMPORARY TABLE %s (
                        window_start TIMESTAMP(3),
                        window_end TIMESTAMP(3),
                        product_id STRING,
                        order_count BIGINT,
                        avg_payment_amount DOUBLE
                    ) WITH (
                        'connector' = 'jdbc',
                        'url' = '%s',
                        'table-name' = 'platform.%s',
                        'username' = '%s',
                        'password' = '%s'
                    );
                    """.formatted(
                    String.format(
                            FlinkSQLPolicy.SINK_TABLE_NAME_CONVENTION,
                            1L
                    ),
                    dbConfig.getUrl(),
                    String.format(
                            PostgreSQLSinkPolicy.RESULT_TABLE_NAME_CONVENTION,
                            1L
                    ),
                    dbConfig.getUsername(),
                    dbConfig.getPassword()
            );

            assertEquals(
                    expected.strip(),
                    actual.strip()
            );
        }

        @Test
        @DisplayName("COUNT(*)의 Sink Result Type은 BIGINT이다")
        void countResultTypeIsBigint() throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context = createContext();

            // when
            String actual = generator.generate(context);

            // then
            assert actual.contains("order_count BIGINT");
        }

        @Test
        @DisplayName("AVG(DOUBLE)의 Sink Result Type은 DOUBLE이다")
        void avgDoubleResultTypeIsDouble() throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context = createContext();

            // when
            String actual = generator.generate(context);

            // then
            assert actual.contains("avg_payment_amount DOUBLE");
        }

        @Test
        @DisplayName("GroupBy 컬럼은 Source Schema 타입을 그대로 사용한다")
        void groupByUsesSourceType() throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context = createContext();

            // when
            String actual = generator.generate(context);

            // then
            assert actual.contains("product_id STRING");
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCases {

        @Test
        @DisplayName("AVG 대상이 DECIMAL이면 현재 MVP에서는 실패한다")
        void avgDecimalFail() throws JsonProcessingException {

            // given
            PostgreSQLSinkDDLGenerationContext context =
                    createContextWithPaymentAmountType(
                            "DECIMAL(18,2)"
                    );

            // when & then
            assertThrows(
                    BaseAPIException.class,
                    () -> generator.generate(context)
            );
        }
    }

    private PostgreSQLSinkDDLGenerationContext createContext()
            throws JsonProcessingException {

        return createContextWithPaymentAmountType("DOUBLE");
    }

    private PostgreSQLSinkDDLGenerationContext createContextWithPaymentAmountType(
            String paymentAmountType
    ) throws JsonProcessingException {

        return PostgreSQLSinkDDLGenerationContext.builder()
                .userId(1L)
                .pipelinePlan(createPipelinePlan())
                .pipeline(createPipeline())
                .parsedTopicSchema(
                        createParsedTopicSchema(paymentAmountType)
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

        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("order_id", "STRING");
        schema.put("product_id", "STRING");
        schema.put("payment_amount", paymentAmountType);
        schema.put("quantity", "INT");
        schema.put("event_time", "TIMESTAMP(3)");

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
                  "filters": [
                    {
                      "field": "payment_amount",
                      "operator": "GTE",
                      "value": 10000
                    }
                  ]
                }
                """;

        return jsonMapper.readValue(
                sourceJson,
                PipelinePlan.class
        );
    }
}