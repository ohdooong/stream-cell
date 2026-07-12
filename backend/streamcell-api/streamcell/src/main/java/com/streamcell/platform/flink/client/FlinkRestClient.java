package com.streamcell.platform.flink.client;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.flink.config.FlinkProperties;
import com.streamcell.platform.flink.dto.FlinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class FlinkRestClient {

    private final FlinkProperties flinkProperties;

    public FlinkResponse.ClusterOverview getClusterOverview() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<FlinkResponse.ClusterOverview> response =
                    restTemplate.exchange(flinkProperties.getClusterOverviewUrl(), HttpMethod.GET, entity, FlinkResponse.ClusterOverview.class);

            return response.getBody();
        } catch (HttpClientErrorException e) {
            // 4xx 에러 처리 (예: 잘못된 요청, 권한 없음)
            throw new BaseAPIException(ErrorCode.INVALID_REQUEST);
        } catch (HttpServerErrorException e) {
            // 5xx 에러 처리 (예: 외부 서버 에러)
            throw new BaseAPIException(ErrorCode.UNAVAILABLE_FLINK);
        } catch (RestClientException e) {
            // 네트워크 오류, 타임아웃 등
            throw new BaseAPIException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
