package com.streamcell.platform.pipeline.dto;


import com.streamcell.platform.pipeline.enums.PipelineStatus;
import lombok.*;

import java.util.List;
import java.util.Map;

public class PipelineResultQueryResponse {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class Dashboard {

        private Long pipelineId;
        private Long deploymentId;
        private PipelineStatus pipelineStatus;
        
        private List<Column> columns;
        private List<Map<String, Object>> rows;

        @Builder
        public static class Column {
            private String key;
            private String label;
            private String type;
        }
    }
}

