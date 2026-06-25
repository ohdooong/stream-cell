package com.streamcell.global._common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    NOT_FOUND_TOPIC(HttpStatus.NOT_FOUND, "토픽정보를 찾을 수 없습니다."),
    JSON_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 파싱 에러"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, ""),
    INVALID_USER(HttpStatus.BAD_REQUEST, "존재하지 않는 사용자가 포함되어 있습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.status = httpStatus;
        this.message = message;
    }
}
