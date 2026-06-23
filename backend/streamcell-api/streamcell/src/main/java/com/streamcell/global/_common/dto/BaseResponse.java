package com.streamcell.global._common.dto;


import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor(staticName = "of")
@AllArgsConstructor
public class BaseResponse<T> {

    private static final int DEFAULT_SUCCESS_STATUS = 200;
    private static final String DEFAULT_SUCCESS_MESSAGE = "요청 성공";

    private int status;
    private String message;
    private LocalDateTime timestamp;
    private T body;


    public static <T> BaseResponse<T> success(int status, String message, T body) {
        return new BaseResponse<>(status, message, LocalDateTime.now(), body);
    }

    public static <T> BaseResponse<T> success(String message, T body) {
        return new BaseResponse<>(DEFAULT_SUCCESS_STATUS, message, LocalDateTime.now(), body);
    }

    public static <T> BaseResponse<T> success(T body) {
        return new BaseResponse<>(DEFAULT_SUCCESS_STATUS, DEFAULT_SUCCESS_MESSAGE, LocalDateTime.now(), body);
    }

}
