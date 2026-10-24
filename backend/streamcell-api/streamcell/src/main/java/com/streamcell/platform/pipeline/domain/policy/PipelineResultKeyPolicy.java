package com.streamcell.platform.pipeline.domain.policy;

import com.streamcell.platform.ai.dto.PipelinePlan;
import java.util.ArrayList;
import java.util.List;

public class PipelineResultKeyPolicy {

    private PipelineResultKeyPolicy() {}

    public static List<String> getPrimaryKeyColumns(PipelinePlan pipelinePlan) {

        List<String> columns = new ArrayList<>();
        columns.add("window_start");
        columns.add("window_end");

        if (pipelinePlan.getGroupBy() != null) {
            columns.addAll(pipelinePlan.getGroupBy());
        }

        return List.copyOf(columns);
    }

}
