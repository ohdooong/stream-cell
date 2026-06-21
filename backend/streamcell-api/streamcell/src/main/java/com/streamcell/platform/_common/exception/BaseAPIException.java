package com.streamcell.platform._common.exception;

import com.streamcell.platform._common.enums.ErrorCode;

public class BaseAPIException extends RuntimeException {
    public BaseAPIException(String message) {
        super(message);
    }

    public BaseAPIException(ErrorCode errorCode) {
        super(errorCode.getMessage());
    }
}
