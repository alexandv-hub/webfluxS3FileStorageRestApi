package com.example.webfluxS3FileStorageRestApi.exception.security;

public class AuthException extends ApiException{
    public AuthException(String message, String errorCode) {
        super(message, errorCode);
    }
}
