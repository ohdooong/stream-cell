package com.streamcell.global._common.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseVO {
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
