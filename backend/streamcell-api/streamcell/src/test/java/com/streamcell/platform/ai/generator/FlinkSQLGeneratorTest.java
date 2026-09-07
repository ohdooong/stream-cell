package com.streamcell.platform.ai.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.platform.ai.domain.FlinkSQLGenerationContext;
import com.streamcell.platform.ai.domain.generator.FlinkSQLGenerator;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.topic.enums.MessageFormat;
import com.streamcell.platform.topic.vo.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

public class FlinkSQLGeneratorTest {

    @Test
    void Select문_테스트() throws JsonProcessingException {
        FlinkSQLGenerator sqlGenerator = new FlinkSQLGenerator();

        PipelinePlan pipelinePlan = createPipelinePlan();
        FlinkSQLGenerationContext context = new FlinkSQLGenerationContext();
        context.setPipelinePlan(pipelinePlan);

        context.setSourceTopic(Topic.builder()
            .topicId(1L)
            .topicName("orders")
            .displayName("주문 이벤트1")
            .description("주문 발생 시 생성되는 샘플 c123")
            .schemaJson("{\"user_id\": \"STRING\", \"order_id\": \"STRING\", \"event_time\": \"TIMESTAMP(3)\", \"product_id\": \"STRING\", \"payment_amount\": \"DECIMAL(10,2)\"}")
            .timeField("event_time")
            .messageFormat(MessageFormat.JSON)
            .build());

        String result = sqlGenerator.generate(context);

        System.out.println("result = " + result);
    }

    PipelinePlan createPipelinePlan() throws JsonProcessingException {
        JsonMapper jsonMapper = new JsonMapper();
        // given
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
                },
                {
                  "field": "order_count",
                  "operator": "GT",
                  "value": 10
                }
              ]
            }
        """;

        // when
        return jsonMapper.readValue(sourceJson, PipelinePlan.class);
    }

}
