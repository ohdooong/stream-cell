package com.streamcell.global._common.file.dto;

import lombok.*;

public class FileResponse {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "from")
    public static class FileUpload {
        private String originalFileName;
        private String savedFileName;
        private String savedPath;
        private long fileSize;
    }
}
