package com.example.webfluxS3FileStorageRestApi.service;

import com.example.webfluxS3FileStorageRestApi.dto.UploadedFileResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface FileStorageService {

    Mono<UploadedFileResponseDTO> uploadUserFileToStorage(FilePart filePart, Mono<Authentication> authMono);

    Mono<ResponseEntity<Resource>> downloadFileFromStorageByFileNameAndAuth(String fileName, Mono<Authentication> authMono);
}
