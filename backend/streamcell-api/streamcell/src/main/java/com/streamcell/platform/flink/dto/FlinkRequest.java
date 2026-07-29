package com.streamcell.platform.flink.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

public class FlinkRequest {

    @Getter
    @Setter
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    @Builder
    public static class JarRun {
        @Schema(description = "Flink jar파일의 job 시작지점.", example = "com.stremacell.jobs.OrderFraudDetectionJob")
        private String entryClass;

        @Schema(description = "병렬도", example = "2")
        private Integer parallelism;

        @Schema(description = "Flink Job arguments", example = "{\"--env\", \"local\", \"--input-topic\", \"orders\"}")
        List<String> programArgsList;
    }
}
