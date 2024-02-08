package com.example.webfluxS3FileStorageRestApi.rest;

import com.example.webfluxS3FileStorageRestApi.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/file-storage")
@Tag(name = "File-Storage", description = "Operations related to file-storage")
public class FileStorageRestControllerV1 {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload-flux", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a file to AWS S3 by user ID",
            description = "Uploads a file to AWS S3 with the specified user ID"
    )
    public Mono<ResponseEntity<String>> uploadFile(@RequestPart("file") Mono<FilePart> filePartMono, Mono<Authentication> authMono) {
        return filePartMono.flatMap(filePart ->
                fileStorageService.uploadUserFileToStorage(filePart, authMono)
        );
    }

    @GetMapping("/download-flux/{fileName}")
    @Operation(
            summary = "Download a file from AWS S3 by filename",
            description = "Downloads a file from AWS S3 with the specified filename"
    )
    public Mono<ResponseEntity<Resource>> downloadFileByName(@PathVariable String fileName, Mono<Authentication> authMono) {
        return fileStorageService.downloadFileFromStorageByFileNameAndAuth(fileName, authMono);
    }
}
