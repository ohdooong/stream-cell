package com.streamcell.platform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.ai.enums.WindowType;
import com.streamcell.platform.ai.enums.WindowUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class DeserializeTest {

    private static final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void PipelinePlan_역직렬화_검증() throws JsonProcessingException {
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
                }
              ]
            }
        """;

        // when
        PipelinePlan pipelinePlan = jsonMapper.readValue(sourceJson, PipelinePlan.class);
        System.out.println("pipelinePlan = " + pipelinePlan);


        // then
        Assertions.assertThat(pipelinePlan)
            .isNotNull();

        Assertions.assertThat(pipelinePlan.getSourceTopicId())
            .isEqualTo(1);
        Assertions.assertThat(pipelinePlan.getWindow())
            .isNotNull();
        Assertions.assertThat(pipelinePlan.getWindow().getType())
            .isEqualTo(WindowType.TUMBLE);
        Assertions.assertThat(pipelinePlan.getWindow().getSize())
            .isEqualTo(5);
        Assertions.assertThat(pipelinePlan.getWindow().getUnit())
            .isEqualTo(WindowUnit.MINUTE);
    }

}
