package com.streamcell.platform.ai.domain.policy;

public class FlinkSQLPolicy {
    public static final int DEFAULT_WATERMARK_DELAY_SECONDS = 5;
    public static final String SOURCE_TABLE_NAME_CONVENTION = "source_p_%s_t_%s";
    public static final String SINK_TABLE_NAME_CONVENTION = "sink_p_%s";
}
