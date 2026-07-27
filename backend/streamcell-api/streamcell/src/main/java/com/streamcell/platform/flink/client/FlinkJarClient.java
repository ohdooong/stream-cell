package com.streamcell.platform.flink.client;

import com.streamcell.platform.flink.config.FlinkProperties;
import com.streamcell.platform.flink.dto.FlinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlinkJarClient {

    private final FlinkProperties flinkProperties;

    public FlinkResponse.JarUploadResponse uploadJar() {
        return FlinkResponse.JarUploadResponse.from();
    }
}


