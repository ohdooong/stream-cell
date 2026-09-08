package com.streamcell.platform.ai.domain.generator;

import com.streamcell.platform.ai.domain.context.KafkaSourceDDLGenerationContext;
import com.streamcell.platform.ai.domain.policy.FlinkSQLPolicy;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.topic.vo.Topic;
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
        //return generateColumns(context);
        // CREATE TABLE -> 영구
        // CREATE TEMPORARY TABLE -> pipeline실행중 일시적으로
        return "CREATE TEMPORARY TABLE " + generateSourceTableName(context) + " (\n    "
                + generateColumns(context)
                + ") WITH (\n    "
                + generateConnectorOptions(context)
                + ");";
    }

    public String generateSourceTableName(KafkaSourceDDLGenerationContext context) {
        Topic sourceTopic = context.getSourceTopic();
        Pipeline pipeline = context.getPipeline();

        // source_p_%s_t_%s -> source_p_1_t_1
        return String.format(FlinkSQLPolicy.SOURCE_TABLE_NAME_CONVENTION,
                pipeline.getPipelineId(),
                sourceTopic.getTopicId());
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

        return String.join(",\n    ", ddlItems)
                + ",\n    "
                + generateWatermark(context);
    }

    private String generateWatermark(KafkaSourceDDLGenerationContext context) {

        Topic sourceTopic = context.getSourceTopic();

        return "WATERMARK FOR " + sourceTopic.getTimeField()
                + " AS " + sourceTopic.getTimeField() + " - INTERVAL "
                + "'"
                + FlinkSQLPolicy.DEFAULT_WATERMARK_DELAY_SECONDS
                + "' SECOND";
    }

    private String generateConnectorOptions(KafkaSourceDDLGenerationContext context) {
        Topic sourceTopic = context.getSourceTopic();
        Pipeline pipeline = context.getPipeline();
        return """
               'connector' = 'kafka',
               'topic' = '%s',
               'properties.bootstrap.servers' = '%s',
               'properties.group.id' = 'streamcell-pipeline-%s',
               'scan.startup.mode' = 'latest-offset',
               'format' = 'json'
               """.formatted(
                       sourceTopic.getTopicName(),
                       String.join(",", kafkaProperties.getBootstrapServers()),
                       pipeline.getPipelineId());
    }

}