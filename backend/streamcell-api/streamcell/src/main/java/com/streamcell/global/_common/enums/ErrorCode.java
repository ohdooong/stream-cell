package com.streamcell.global._common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 4xx 에러
    NOT_FOUND_TOPIC(HttpStatus.NOT_FOUND, "토픽정보를 찾을 수 없습니다."),
    NOT_FOUND_PIPELINE(HttpStatus.NOT_FOUND,"파이프라인을 찾을 수 없습니다"),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_USER(HttpStatus.BAD_REQUEST, "존재하지 않는 사용자가 포함되어 있습니다."),
    INVALID_ENTRY_CLASS(HttpStatus.BAD_REQUEST, "유효하지않은 entry class 경로입니다."),
    INVALID_PARALLELISM(HttpStatus.BAD_REQUEST, "병렬도는 1 ~ 8사이값만 입력가능합니다."),

    FORBIDDEN_TOPICS(HttpStatus.FORBIDDEN, "접근 불가능한 Topic이 포함되어 있습니다."),

    CONFLICT_PIPLINE_ARTIFACT(HttpStatus.CONFLICT, "artifact가 이미 존재합니다."),
    CONFLICT_CUSTOM_JOB_CONFIG(HttpStatus.CONFLICT, "custom job 설정이 이미 존재합니다."),


    // 5xx 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류입니다."),
    FAILED_FILE_SAVE(HttpStatus.INSUFFICIENT_STORAGE, "파일 저장에 실패했습니다."),
    JSON_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 파싱 에러"),

    UNAVAILABLE_FLINK(HttpStatus.SERVICE_UNAVAILABLE, "Flink 서버 에러입니다.")
    ;

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.status = httpStatus;
        this.message = message;
    }
}
