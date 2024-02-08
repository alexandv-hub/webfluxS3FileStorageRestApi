package com.example.webfluxS3FileStorageRestApi.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface FileStorageService {

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Mono<ResponseEntity<String>> uploadUserFileToStorage(FilePart filePart, Mono<Authentication> authMono);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Mono<ResponseEntity<Resource>> downloadFileFromStorageByFileNameAndAuth(String fileName, Mono<Authentication> authMono);
}
