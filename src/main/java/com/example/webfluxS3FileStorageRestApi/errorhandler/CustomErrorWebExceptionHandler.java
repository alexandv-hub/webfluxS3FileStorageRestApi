package com.example.webfluxS3FileStorageRestApi.errorhandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(-2) // Ensure this handler has precedence over the default one
public class CustomErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Autowired
    public CustomErrorWebExceptionHandler(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            Map<String, Object> errorPropertiesMap = new HashMap<>();
            errorPropertiesMap.put("status", responseStatusException.getStatusCode().value());
            errorPropertiesMap.put("error", responseStatusException.getStatusCode());
            errorPropertiesMap.put("message", responseStatusException.getReason());
            errorPropertiesMap.put("requestId", exchange.getRequest().getId());
            errorPropertiesMap.put("path", exchange.getRequest().getPath().value());
            errorPropertiesMap.put("timestamp", Instant.now().toString());

            try {
                byte[] errorBytes = objectMapper.writeValueAsBytes(errorPropertiesMap);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(errorBytes)));
            } catch (JsonProcessingException e) {
                return Mono.error(e);
            }
        }
        return Mono.error(ex);
    }
}
