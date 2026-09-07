package com.streamcell.platform.ai.domain.generator;

import com.streamcell.platform.ai.domain.context.KafkaSourceDDLGenerationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaSourceDDLGenerator {

    private final KafkaProperties kafkaProperties;

    public String generate(KafkaSourceDDLGenerationContext context) {
        return generateColumns(context);
    }

    private String generateWatermark(KafkaSourceDDLGenerationContext context) {
        return "";
    }

    private String generateColumns(KafkaSourceDDLGenerationContext context) {
        Map<String, Object> parsedTopicSchema = context.getParsedTopicSchema();

        List<String> ddlItems = new ArrayList<>();
        for (Map.Entry<String, Object> entry : parsedTopicSchema.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key == null || value == null) {
                continue;
            }

            ddlItems.add(key + " " + value);
        }

        return String.join("\n,    ", ddlItems);
    }

    public String generateSourceTableName(KafkaSourceDDLGenerationContext context) {
        return "";
    }




}