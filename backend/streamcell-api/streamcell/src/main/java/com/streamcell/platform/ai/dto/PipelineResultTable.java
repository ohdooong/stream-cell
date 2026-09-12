package com.streamcell.platform.ai.dto;

import lombok.*;

@Getter
@Setter
@Builder
public class PipelineResultTable {
    private String tableName;
    private String columns;

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "from")
    public static class Response {
        private final String createdTableName;
    }
}