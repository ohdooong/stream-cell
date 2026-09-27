package com.streamcell.platform.ai.dto;

import lombok.*;

public class AIClientRequest {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class CreateSession {
        private String title;
    }

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor(staticName = "from")
    @NoArgsConstructor(staticName = "from")
    public static class Message {
        /* 사용자 메세지 */
        private String message;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class GeneratePipelinePlan {

    }



}
