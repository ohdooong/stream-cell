package com.streamcell.platform.flink.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
        @JsonProperty("filename")
        private String filename;
        @JsonProperty("status")
        private String status;
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class JarRunResponse {
        @JsonProperty("jobid")
        private String jobId;
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class JobStatus {
        @JsonProperty("status")
        private String status;
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class JobExceptionsHistory {
        private List<JobExceptionsEntry> exceptionEntries;
        private boolean truncated;
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class JobExceptionsEntry {
        private String exceptionName;
        private String stacktrace;
        private Long timestamp;
        private String taskName;
        private String taskManagerId;
    }

}

