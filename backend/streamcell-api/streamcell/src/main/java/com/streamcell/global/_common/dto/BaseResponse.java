package com.streamcell.global._common.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
public class BaseResponse<T> {

    private int status;
    private String message;
    private LocalDateTime timestamp;
    private T body;

    public static <T> BaseResponse<T> success(String message, T body) {
        return new BaseResponse<>(200, message, LocalDateTime.now(), body);
    }

    public static <T> BaseResponse<T> success(T body) {
        return new BaseResponse<>(200, "요청 성공", LocalDateTime.now(), body);
    }

    public static <T> BaseResponse<T> error(int status, String message, T body) {
        return new BaseResponse<>(status, message, LocalDateTime.now(), body);
    }

    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>(500, message, LocalDateTime.now(), null);
    }
}
