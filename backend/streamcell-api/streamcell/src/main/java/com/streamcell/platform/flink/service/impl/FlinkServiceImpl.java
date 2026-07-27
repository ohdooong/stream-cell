package com.streamcell.platform.flink.service.impl;

import com.streamcell.platform.flink.client.FlinkRestClient;
import com.streamcell.platform.flink.dto.FlinkResponse;
import com.streamcell.platform.flink.service.FlinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlinkServiceImpl implements FlinkService {

    private final FlinkRestClient flinkRestClient;

    @Override
    public FlinkResponse.ClusterOverview getClusterOverview() {
        return flinkRestClient.getClusterOverview();
    }


}
