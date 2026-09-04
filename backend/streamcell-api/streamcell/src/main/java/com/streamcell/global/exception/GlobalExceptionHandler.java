package com.streamcell.global.exception;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.global._common.exception.BaseAPIException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<BaseResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
//        String message = exception.getMessage();
//        BaseResponse<Object> error = BaseResponse.error("시스템 에러입니다.\n관리자에게 문의하세요.");
//        return ResponseEntity.status(500).body(error);
//    }

    @ExceptionHandler(BaseAPIException.class)
    public ResponseEntity<BaseResponse<?>> handleBaseAPIException(BaseAPIException exception) {
        log.error(Arrays.toString(exception.getStackTrace()));
        String message = exception.getMessage();
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(BaseResponse.error(exception.getErrorCode(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception exception) {
        log.error(Arrays.toString(exception.getStackTrace()));
        String message = "시스템 오류입니다.\\n관리자에게 문의하세요.";
        return ResponseEntity.internalServerError()
            .body(BaseResponse.error(message));
    }
}
