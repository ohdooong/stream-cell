package com.streamcell.platform.flink.service;

import com.streamcell.platform.flink.dto.FlinkResponse;

public interface FlinkService {

    FlinkResponse.ClusterOverview getClusterOverview();

}
