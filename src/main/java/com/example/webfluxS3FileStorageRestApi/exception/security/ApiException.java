package com.example.webfluxS3FileStorageRestApi.exception.security;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException{

    protected String errorCode;

    public ApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
