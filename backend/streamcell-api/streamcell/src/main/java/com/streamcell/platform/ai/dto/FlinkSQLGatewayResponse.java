package com.streamcell.platform.ai.dto;

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
    public static class FetchJobId {
        private String jobId;
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
