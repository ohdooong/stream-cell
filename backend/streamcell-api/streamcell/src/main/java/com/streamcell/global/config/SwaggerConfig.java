package com.streamcell.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.kafka.support.converter.JacksonJsonMessageConverter;
import org.springframework.kafka.support.converter.MappingJacksonJsonParameterizedConverter;
import org.springframework.kafka.support.mapping.JacksonJavaTypeMapper;

@Configuration
public class SwaggerConfig {

//    public SwaggerConfig(JacksonJsonMessageConverter converter) {
//
//
//    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("StreamCell Backend API")
                .description("StreamCell Backend API Swagger입니다.")
                .version("1.0.0");
    }
}
