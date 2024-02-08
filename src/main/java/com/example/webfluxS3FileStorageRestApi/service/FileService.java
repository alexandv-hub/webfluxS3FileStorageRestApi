package com.example.webfluxS3FileStorageRestApi.service;

import com.example.webfluxS3FileStorageRestApi.dto.FileDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileService {

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Mono<FileDTO> getFileByIdAndAuth(Long fileId, Mono<Authentication> authMono);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Mono<Boolean> existsByIdAndUserId(Long fileId, Long userId);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Flux<FileDTO> getAllFiles(Mono<Authentication> authMono);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    Mono<FileDTO> updateFile(FileDTO fileDTO);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    Mono<Void> deleteFileById(Long Id);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    Mono<Integer> deleteAllFilesByUserId(Long userId);

    @PreAuthorize("hasAnyRole('ADMIN')")
    Mono<Integer> deleteAllFiles();
}
