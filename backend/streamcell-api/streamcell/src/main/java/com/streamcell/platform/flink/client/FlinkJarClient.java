package com.streamcell.platform.flink.client;

import com.streamcell.platform.flink.config.FlinkProperties;
import com.streamcell.platform.flink.dto.FlinkResponse;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class FlinkJarClient {

    private final RestClient restClient;

    private final FlinkProperties flinkProperties;

    public FlinkResponse.JarUploadResponse uploadJar(PipelineArtifact artifact) {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(artifact.getStoredFilePath()))
                .header(HttpHeaders.CONTENT_TYPE, "application/x-java-archive");
        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

        return restClient.post()
                .uri(flinkProperties.getUploadJarUrl())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipartBody)
                .retrieve()
                .body(FlinkResponse.JarUploadResponse.class);
    }

}


