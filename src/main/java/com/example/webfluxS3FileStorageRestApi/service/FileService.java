package com.example.webfluxS3FileStorageRestApi.service;

import com.example.webfluxS3FileStorageRestApi.model.File;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileService {

    Mono<File> getFileByIdAndAuth(Long fileId, Mono<Authentication> authMono);

    Mono<Boolean> existsByIdAndUserId(Long fileId, Long userId);

    Flux<File> getAllFilesByAuth(Mono<Authentication> authMono);

    Flux<File> getFilesByUserId(Long userId);

    Mono<File> updateFileById(Long id, File file);

    Mono<Void> deleteFileById(Long Id);

    Mono<Integer> deleteAllFilesByUserId(Long userId);

    Mono<Integer> deleteAllFiles();
}
