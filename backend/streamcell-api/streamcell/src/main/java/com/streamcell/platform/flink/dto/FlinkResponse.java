package com.streamcell.platform.flink.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

public class FlinkResponse {

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class ClusterOverview {
        @JsonProperty("flink-version")
        private String flinkVersion;

        @JsonProperty("taskmanagers")
        private Integer taskManagers;

        @JsonProperty("slots-total")
        private Integer slotsTotal;

        @JsonProperty("slots-available")
        private Integer slotsAvailable;

        @JsonProperty("jobs-running")
        private Integer jobsRunning;

        @JsonProperty("jobs-finished")
        private Integer jobsFinished;

        @JsonProperty("jobs-failed")
        private Integer jobsFailed;

        @JsonProperty("jobs-cancelled")
        private Integer jobsCancelled;
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class JarUploadResponse {

    }

}

