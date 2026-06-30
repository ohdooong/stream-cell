package com.streamcell.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Configuration
public class WebConfig {

    @Bean
    public JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter() {
        JsonMapper build = JsonMapper.builder().build();
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(build);
        converter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_OCTET_STREAM
        ));
        return converter;
    }
}
