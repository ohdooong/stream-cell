package com.streamcell.platform.ai.domain.policy;

import com.streamcell.platform.ai.domain.spec.AggregationSpec;
import com.streamcell.platform.ai.dto.PipelinePlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostgreSQLSinkPolicy {
    public static String RESULT_TABLE_NAME_CONVENTION = "pipeline_result_%s";
}
