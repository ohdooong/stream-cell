package com.streamcell.global._common.constants;

public class SecurityConstants {

    public static final String LOGIN_API_END_POINT = "/api/v1/web";
    public static final String LOGOUT_API_END_POINT = "";


    // 인증이 필요하지 않은 웹리소스 엔드포인트를 등록한다.
    public static final String[] RESOURCES_END_POINTS = new String[]{
        "/favicon.ico",
        "/resources/**",
        "/fonts/**",
        "/static/**",
        "/css/**",
        "/js/**",
        "/images/**",
        "/webjars/**",
        "/.well-known/**",
        "/preview/mail/**",
        "/mail/**",
    };

    // 웹리소스는 아니지만, 인증과 관계없는 엔드포인트를 등록한다.
    public static final String[] PERMIT_ALL_END_POINTS = new String[]{
        // Swagger UI와 관련된 URI //
        "/configuration/ui/**",
        "/swagger-ui/**", //
        "/swagger-ui.html", //
        "/v3/api-docs**",
        "/v3/api-docs/**",
        "/api-docs/**", //
        // 그 외 제외 대상
        "/", //
        "/index.html", //
        "/csrf",
        "/error", //
        "/error/**", //
        LOGIN_API_END_POINT, // 로그인 URI
        LOGOUT_API_END_POINT, // 로그아웃 URI
    };
}
