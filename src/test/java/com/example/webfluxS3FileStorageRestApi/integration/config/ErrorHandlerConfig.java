package com.example.webfluxS3FileStorageRestApi.integration.config;

import com.example.webfluxS3FileStorageRestApi.errorhandler.AppErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorHandlerConfig {

    @Bean
    public AppErrorAttributes appErrorAttributes() {
        return new AppErrorAttributes();
    }
}
