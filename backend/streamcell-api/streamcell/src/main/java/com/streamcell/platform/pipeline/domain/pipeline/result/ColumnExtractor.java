package com.streamcell.platform.pipeline.domain.pipeline.result;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;
import com.streamcell.platform.pipeline.dto.PipelineResultQueryResponse;
import com.streamcell.platform.pipeline.enums.ColumnType;
import com.streamcell.platform.topic.vo.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.streamcell.platform.pipeline.enums.ColumnType.NUMBER;
import static com.streamcell.platform.pipeline.enums.ColumnType.TIMESTAMP;

@Component
@RequiredArgsConstructor
public class ColumnExtractor {

    private final ColumnTypeConverter columnTypeConverter;

    private final JsonMapper jsonMapper = new JsonMapper();

    public List<PipelineResultQueryResponse.Dashboard.Column> extract(PipelinePlan pipelinePlan, Topic topic) {
        List<PipelineResultQueryResponse.Dashboard.Column> columns = new ArrayList<>();

        PipelineResultQueryResponse.Dashboard.Column windowStart =
                PipelineResultQueryResponse.Dashboard.Column.from("window_start", TIMESTAMP);
        columns.add(windowStart);

        PipelineResultQueryResponse.Dashboard.Column windowEnd =
                PipelineResultQueryResponse.Dashboard.Column.from("window_end", TIMESTAMP);
        columns.add(windowEnd);

        Map<String, Object> parsedTopicSchema = getParsedTopicSchema(topic);
        for (String item : pipelinePlan.getGroupBy()) {
            String dataType = (String) parsedTopicSchema.get(item);
            ColumnType columnType = columnTypeConverter.convertColumnType(dataType);

            columns.add(
                PipelineResultQueryResponse.Dashboard.Column.from(item, columnType)
            );
        }

        for (AggregationSpec aggregation : pipelinePlan.getAggregations()) {
            String alias = aggregation.getAlias();

            columns.add(
                    PipelineResultQueryResponse.Dashboard.Column.from(alias, NUMBER)
            );
        }

        return columns;
    }


    private Map<String, Object> getParsedTopicSchema(Topic topic) {
        Map<String, Object> parsedTopicSchema;
        try {
            parsedTopicSchema =
                    // 스키마 순서를 위해 LinkedHashMap으로 변경
                    jsonMapper.readValue(topic.getSchemaJson(), new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return parsedTopicSchema;
    }

}
