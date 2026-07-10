package com.streamcell.global.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.streamcell.**", annotationClass = Mapper.class)
public class MyBatisConfig {

}
