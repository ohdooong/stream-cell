package com.streamcell.platform.flink.client;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.flink.config.FlinkProperties;
import com.streamcell.platform.flink.dto.FlinkResponse;
import com.streamcell.platform.flink.enums.FlinkJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlinkRestClient {

    private final RestClient restClient;

    private final FlinkProperties flinkProperties;


    /**
     * flink cluster 정보 조회
     * @return
     */
    public FlinkResponse.ClusterOverview getClusterOverview() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<FlinkResponse.ClusterOverview> response =
                    restTemplate.exchange(
                            flinkProperties.getClusterOverviewUrl(),
                            HttpMethod.GET,
                            entity,
                            FlinkResponse.ClusterOverview.class);

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

    /**
     * flinkJob의 상태를 조회
     * @param flinkJobId
     * @return
     */
    public FlinkJobStatus getJobStatus(String flinkJobId) {
        if (flinkJobId == null || flinkJobId.isBlank()) {
            throw new BaseAPIException(ErrorCode.INVALID_FLINK_JOB_ID);
        }

        FlinkResponse.JobStatus body = restClient.get()
                .uri(String.format(flinkProperties.getJobStatus(), flinkJobId))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (httpRequest, httpResponse) -> {
                    throw new BaseAPIException(ErrorCode.NOT_FOUND_FLINK_JOB_ID_FROM_CLUSTER);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (httpRequest, httpResponse) -> {
                    throw new BaseAPIException(ErrorCode.UNAVAILABLE_FLINK);
                })
                .body(FlinkResponse.JobStatus.class);
        return Optional.ofNullable(FlinkJobStatus.from(body.getStatus()))
                .orElseThrow(() -> new BaseAPIException(ErrorCode.INVALID_FLINK_JOB_STATUS));
    }

    /**
     * flink job을 cancel 요청한다.
     * @param flinkJobId
     * @return
     */
    public FlinkJobStatus cancelJob(String flinkJobId) {
        if (flinkJobId == null || flinkJobId.isBlank()) {
            throw new BaseAPIException(ErrorCode.INVALID_FLINK_JOB_ID);
        }

        try {
             org.springframework.http.ResponseEntity<String> responseResult = restClient.post()
                    .uri(String.format(flinkProperties.getCancelJobUrl(), flinkJobId))
                    .retrieve()
                    .onStatus(status -> !status.isSameCodeAs(HttpStatusCode.valueOf(202))
                            , (request, response) -> {
                                throw new BaseAPIException(ErrorCode.FAILED_CANCEL_JOB);
                            })
                    .toEntity(String.class);

            log.info("responseResult headers: {}", responseResult.getHeaders());


            return FlinkJobStatus.CANCELLING;
        } catch (Exception e) {
            throw new BaseAPIException(ErrorCode.FAILED_CANCEL_JOB);
        }
    }


    public FlinkResponse.JobExceptionsHistory getExceptionsByJobId(String flinkJobId) {
        if (flinkJobId == null || flinkJobId.isBlank()) {
            throw new BaseAPIException(ErrorCode.INVALID_FLINK_JOB_ID);
        }

        return restClient.get()
            .uri(String.format(flinkProperties.getExceptionsUrl(), flinkJobId))
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                throw new BaseAPIException(ErrorCode.INVALID_REQUEST);
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new BaseAPIException(ErrorCode.UNAVAILABLE_FLINK);
            })
            .body(FlinkResponse.JobExceptionsHistory.class);
    }
}
