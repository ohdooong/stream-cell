package com.streamcell.platform.flink.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class FlinkRequest {


    @Getter
    @Setter
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    @Builder
    public static class JarRun {

        private String entryClass;

        private Integer parallelism;

        List<String> programArgsList;
    }
}
