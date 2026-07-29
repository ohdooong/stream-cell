package com.streamcell.platform.flink.client;

import com.streamcell.platform.flink.config.FlinkProperties;
import com.streamcell.platform.flink.dto.FlinkRequest;
import com.streamcell.platform.flink.dto.FlinkResponse;
import com.streamcell.platform.pipeline.vo.CustomJobConfig;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public FlinkResponse.JarRunResponse runJar(Pipeline pipeline, PipelineArtifact artifact, CustomJobConfig customJobConfig) {
        FlinkRequest.JarRun request = FlinkRequest.JarRun
                .builder()
                .entryClass(customJobConfig.getEntryClass())
                .parallelism(customJobConfig.getParallelism())
                .programArgsList(convertProgramArgs(customJobConfig.getProgramArgs()))
                .build();

        return restClient.post()
                .uri(String.format(flinkProperties.getRunJarUrl(), artifact.getFlinkJarId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(FlinkResponse.JarRunResponse.class);
    }

    private List<String> convertProgramArgs(Map<String, String> programArgsMap) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : programArgsMap.entrySet()) {
            list.add(entry.getKey());
            list.add(entry.getValue());
        }
        return list;
    }

}


