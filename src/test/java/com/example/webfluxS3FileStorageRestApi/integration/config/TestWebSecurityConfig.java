package com.example.webfluxS3FileStorageRestApi.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@TestConfiguration
public class TestWebSecurityConfig {

    private final String [] publicRoutes = {"/api/v1/auth/register", "/api/v1/auth/login"};

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(publicRoutes).permitAll()
                        .anyExchange().permitAll()).build();
    }
}
