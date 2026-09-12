package com.streamcell.platform.flink.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public enum EndPoint {
    OVERVIEW("/overview"),
    UPLOAD_JAR("/jars/upload"),
    RUN_JAR("/jars/%s/run", "Flink Jar을 Run /jars/{flinkJarId}/run"),

    CANCEL_JOB("/jobs/%s", "job 종료 요청. 202 Accepted 반환."),
    JOB_STATUS("/jobs/%s/status", "Flink Job status조회 /jobs/{flinkJobId}/status"),

    GET_EXCEPTIONS("/jobs/%s/exceptions", "실패한 Job의 Exceptions를 조회한다. /jobs/{flinkJobId}/exceptions"),

    CREATE_SESSION("/sessions", "session 생성 : Opens a new session with specific properties." + "sessionHandle반환"),
    CLOSE_SESSION("/sessions/%s","/sessions/:session_handle session 종료 : Closes the specific session."),
    CREATE_SOURCE_OR_SINK("/sessions/%s/configure-session", "/sessions/:session_handle/configure-session -> Configures the session with the statement which could be:" +
            " CREATE TABLE, DROP TABLE, ALTER TABLE, CREATE DATABASE, DROP DATABASE, ALTER DATABASE, CREATE FUNCTION, DROP FUNCTION, ALTER FUNCTION, CREATE CATALOG, DROP CATALOG, USE CATALOG," +
            " USE [CATALOG.]DATABASE, CREATE VIEW, DROP VIEW, LOAD MODULE, UNLOAD MODULE, USE MODULE, ADD JAR."),

    SUBMIT_SQL("/sessions/%s/statements", "/sessions/:session_handle/statements -> Execute a statement. ex) INSERT INTO SINK ~ SELECT ~")

    ;


    private final String path;
    private String description;

}
