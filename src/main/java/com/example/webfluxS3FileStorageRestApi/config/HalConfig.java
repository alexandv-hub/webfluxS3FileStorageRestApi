package com.example.webfluxS3FileStorageRestApi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;

import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;
import static org.springframework.hateoas.support.WebStack.WEBFLUX;

@Configuration
@EnableHypermediaSupport(type = HAL, stacks = WEBFLUX)
public class HalConfig {
    // Config for HAL with key "_links" (if WEBFLUX) or "links" (if WEBMVC)
}