package com.example.webfluxS3FileStorageRestApi.repository;

import com.example.webfluxS3FileStorageRestApi.dto.UploadedFileResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface FileStorageRepository {

    Mono<UploadedFileResponseDTO> uploadUserFileToStorage(FilePart filePart);
    Mono<ResponseEntity<Resource>> downloadFileFromStorage(String fileName);
}
