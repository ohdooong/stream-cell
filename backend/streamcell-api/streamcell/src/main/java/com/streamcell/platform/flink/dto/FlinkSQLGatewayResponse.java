package com.streamcell.platform.flink.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamcell.platform.ai.domain.enums.ResultType;
import com.streamcell.platform.flink.enums.OperationStatus;
import lombok.*;

public class FlinkSQLGatewayResponse {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class CreateSession {
        private String sessionHandle;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class CreateSource {
        private String operationHandle;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class CreateSink {
        private String operationHandle;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class SubmitSQL {
        private String operationHandle;
    }


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class FetchResult {
        private ResultType resultType;
        private Boolean isQueryResult;
        @JsonProperty("jobID")
        private String jobId;
        private String resultKind;
        private String nextResultUrl;
//        private Result results;
//
//        public static class Result {
//
//            private List<Column> columns;
//
//            @JsonProperty("data")
//            private List<ResultData> datas;
//
//            public static class Column {
//                private String name;
//            }
//
//            public static class ResultData {
//                private String kind;
//                private List<String> fields;
//            }
//        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class FetchStatus {
        private OperationStatus status;
    }


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class CloseSession {
        private String sessionHandle;
    }


}
