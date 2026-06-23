package com.streamcell.global._common.exception;

import com.streamcell.global._common.enums.ErrorCode;

public class BaseAPIException extends RuntimeException {
    public BaseAPIException(String message) {
        super(message);
    }

    public BaseAPIException(ErrorCode errorCode) {
        super(errorCode.getMessage());
    }
}
