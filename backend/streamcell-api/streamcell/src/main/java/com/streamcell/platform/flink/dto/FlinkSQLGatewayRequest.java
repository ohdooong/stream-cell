package com.streamcell.platform.flink.dto;

import lombok.*;

public class FlinkSQLGatewayRequest {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class CreateSink {
        private String statement;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class CreateSource {
        private String statement;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class SubmitSQL {
        private String statement;
    }
}
