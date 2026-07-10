package com.streamcell.global.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<BaseResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
//        String message = exception.getMessage();
//        BaseResponse<Object> error = BaseResponse.error("시스템 에러입니다.\n관리자에게 문의하세요.");
//        return ResponseEntity.status(500).body(error);
//    }
}
