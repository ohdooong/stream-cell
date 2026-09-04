package com.streamcell.global._common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 4xx 에러
    NOT_FOUND_TOPIC(HttpStatus.NOT_FOUND, "토픽정보를 찾을 수 없습니다."),
    NOT_FOUND_PIPELINE(HttpStatus.NOT_FOUND, "파이프라인을 찾을 수 없습니다"),
    NOT_FOUND_PIPELINE_ARTIFACT(HttpStatus.NOT_FOUND, "파이프라인 Artifact정보를 찾을 수 없습니다."),
    NOT_FOUND_CUSTOM_JOB_CONFIG(HttpStatus.NOT_FOUND, "Custom Job Config정보를 찾을 수 없습니다."),
    NOT_FOUND_FILE(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."),
    NOT_FOUND_PIPELINE_DEPLOYMENT(HttpStatus.NOT_FOUND, "Pipeline Deployment 정보를 찾을 수 없습니다."),
    NOT_FOUND_FLINK_JOB_ID(HttpStatus.NOT_FOUND, "Flink Job ID가 존재하지 않습니다. 정상적으로 배포되었는지 확인하십시오."),
    NOT_FOUND_FLINK_JOB_ID_FROM_CLUSTER(HttpStatus.NOT_FOUND, "Flink Cluster에서 Flink Job 정보를 찾을 수 없습니다."),
    NOT_FOUND_TOPIC_SCHEMA(HttpStatus.NOT_FOUND, "해당 토픽의 스키마 정보를 찾을 수 없습니다. Topic Id: %s"),
    NOT_FOUND_TOPIC_TIME_FIELD(HttpStatus.NOT_FOUND, "해당 토픽의 Event Time Field를 찾을 수 없습니다. Topic Id: %s"),
    NOT_FOUND_WINDOW_SPEC(HttpStatus.NOT_FOUND, "Window Spec을 찾을 수 없습니다."),
    NOT_FOUND_WINDOW_TYPE(HttpStatus.NOT_FOUND, "Window Type값이 없습니다."),
    NOT_FOUND_WINDOW_UNIT(HttpStatus.NOT_FOUND, "Window Unit값이 없습니다."),
    NOT_FOUND_WINDOW_SIZE(HttpStatus.NOT_FOUND, "Window Size값이 없습니다."),
    NOT_FOUND_AGGREGATIONS(HttpStatus.NOT_FOUND, "aggregations값을 찾을 수 없습니다."),
    NOT_FOUND_AGGREGATION_FUNCTION(HttpStatus.NOT_FOUND, "aggregations값을 찾을 수 없습니다."),
    NOT_FOUND_AGGREGATION_FIELD(HttpStatus.NOT_FOUND, "aggregation 필드(field)값을 찾을 수 없습니다."),
    NOT_FOUND_AGGREGATION_ALIAS(HttpStatus.NOT_FOUND, "aggregations 별칭(alias)값을 찾을 수 없습니다."),


    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_USER(HttpStatus.BAD_REQUEST, "존재하지 않는 사용자가 포함되어 있습니다."),
    INVALID_ENTRY_CLASS(HttpStatus.BAD_REQUEST, "유효하지않은 entry class 경로입니다."),
    INVALID_PARALLELISM(HttpStatus.BAD_REQUEST, "병렬도는 1 ~ 8사이값만 입력가능합니다."),
    INVALID_FLINK_JOB_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 Flink Job 상태입니다."),
    INVALID_FLINK_JOB_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 Flink Job ID 입니다."),
    INVALID_CANCEL_FLINK_JOB(HttpStatus.BAD_REQUEST, "Job을 중지할 수 있는 상태가 아닙니다."),
    INVALID_AI_SQL_REQUEST(HttpStatus.BAD_REQUEST, "AI SQL을 요청할 수 있는 Pipeline이 아닙니다. Pipeline 유형을 확인하세요."),
    INVALID_AI_SQL_REQUEST_OPERATOR(HttpStatus.BAD_REQUEST, "해당값에 유효하지않은 Operator입니다."),
    INVALID_AI_SQL_REQUEST_FILTER_VALUE_TYPE(HttpStatus.BAD_REQUEST, "Filter값 타입이 잘못되었습니다."),
    INVALID_PIPELINE_STATUS_FOR_AI_SQL(HttpStatus.BAD_REQUEST, "Pipeline이 최초생성되었을때만 요청가능합니다."),
    INVALID_MESSAGE_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않는 Message Format입니다. Current Message Format: %s"),
    INVALID_PIPELINE_PLAN(HttpStatus.BAD_REQUEST, "유효하지 않은 Pipeline Plan입니다. Pipeline Plan: %s"),
    INVALID_WINDOW_SIZE(HttpStatus.BAD_REQUEST, "Window Size의 허용범위는 0 ~ 30입니다."),
    INVALID_TOPIC_SCHEMA(HttpStatus.BAD_REQUEST, "유효하지 않은 Topic Schema가 포함되어 있습니다."),
    INVALID_FIELD_TYPE(HttpStatus.BAD_REQUEST, "Aggregation 시 숫자형만 가능합니다."),
    INVALID_AGGREGATION_FUNCTION(HttpStatus.BAD_REQUEST, "유효하지 않은 Aggregation Function입니다."),

    NOT_IMPLEMENTED_WINDOW_TYPE(HttpStatus.NOT_IMPLEMENTED, "아직 지원하지 않는 Window Type 입니다."),
    NOT_IMPLEMENTED_WINDOW_UNIT(HttpStatus.NOT_IMPLEMENTED, "아직 지원하지 않는 Window Unit 입니다."),
    NOT_IMPLEMENTED_FIELD_TYPE(HttpStatus.NOT_IMPLEMENTED, "아직 지원하지 않는 Field Type 입니다. 숫자데이터만 요청해주세요."),

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    BAD_REQUEST_NOT_CUSTOM_JAR_TYPE(HttpStatus.BAD_REQUEST, "Pipeline Type CUSTOM_JAR가 아닙니다."),
    BAD_REQUEST_NOT_UPLOADED_CUSTOM_JAR(HttpStatus.BAD_REQUEST, "Custom Jar파일이 업로드 되지 않았습니다."),

    FAILED_FLINK_DEPLOY(HttpStatus.BAD_REQUEST, "Flink 배포에 실패했습니다."),

    FORBIDDEN_TOPICS(HttpStatus.FORBIDDEN, "접근 불가능한 Topic이 포함되어 있습니다."),

    CONFLICT_PIPLINE_ARTIFACT(HttpStatus.CONFLICT, "artifact가 이미 존재합니다."),
    CONFLICT_CUSTOM_JOB_CONFIG(HttpStatus.CONFLICT, "custom job 설정이 이미 존재합니다."),
    CONFLICT_FLINK_JAR_ID(HttpStatus.CONFLICT, "이미 배포한 Flink jar파일이 존재합니다."),
    CONFLICT_PIPELINE_DEPLOYMENT(HttpStatus.CONFLICT, "pipeline이 배포된 상태입니다."),
    CONFLICT_ALIAS(HttpStatus.CONFLICT, "중복된 별칭(alias)입니다. -> alias: %s"),

    // 5xx 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류입니다."),
    FAILED_FILE_SAVE(HttpStatus.INSUFFICIENT_STORAGE, "파일 저장에 실패했습니다."),
    FAILED_UPLOAD_JAR(HttpStatus.INTERNAL_SERVER_ERROR, "Jar 업로드에 실패했습니다."),
    FAILED_CANCEL_JOB(HttpStatus.INTERNAL_SERVER_ERROR, "Job Cancel에 실패하였습니다."),

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
