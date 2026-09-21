package com.streamcell.global.config;

import com.streamcell.global.interceptor.RestClientLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {

        HttpComponentsClientHttpRequestFactory baseFactory = new HttpComponentsClientHttpRequestFactory();
        BufferingClientHttpRequestFactory bufferingFactory =
                new BufferingClientHttpRequestFactory(baseFactory);

        return RestClient.builder()
                .requestFactory(bufferingFactory)
                .requestInterceptor(new RestClientLoggingInterceptor())
                .build();
    }
}
