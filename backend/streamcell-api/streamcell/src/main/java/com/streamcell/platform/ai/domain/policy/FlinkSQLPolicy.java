package com.streamcell.platform.ai.domain.policy;

public class FlinkSQLPolicy {
    // 워터마크 딜레이
    // 일단, 기본적으로 5초 정책.
    public static final int DEFAULT_WATERMARK_DELAY_SECONDS = 5;
    public static final String SOURCE_TABLE_NAME_CONVENTION = "source_p_%s_t_%s";
    public static final String SINK_TABLE_NAME_CONVENTION = "sink_p_%s";
}
