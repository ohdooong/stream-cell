package com.streamcell.global._common.exception;

import com.streamcell.global._common.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BaseAPIException extends RuntimeException {

    private ErrorCode errorCode;

    public BaseAPIException(String message) {
        super(message);
    }

    public BaseAPIException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BaseAPIException(ErrorCode errorCode, Object reference) {
        super(String.format(errorCode.getMessage(), reference));
        this.errorCode = errorCode;
    }
}
