package com.example.webfluxS3FileStorageRestApi.service.impl;

import com.example.webfluxS3FileStorageRestApi.dto.UploadedFileResponseDTO;
import com.example.webfluxS3FileStorageRestApi.model.Event;
import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.model.UserRole;
import com.example.webfluxS3FileStorageRestApi.repository.EventRepository;
import com.example.webfluxS3FileStorageRestApi.repository.FileRepository;
import com.example.webfluxS3FileStorageRestApi.repository.FileStorageRepository;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.EventService;
import com.example.webfluxS3FileStorageRestApi.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.Collection;
import java.util.Collections;

import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.ERR_ACCESS_DENIED;
import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.ERR_INVALID_AUTHENTICATION;
import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.FileStorage.*;
import static com.example.webfluxS3FileStorageRestApi.messages.InfoMessages.FileStorage.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.s3.bucket-name}")
    private String s3BucketName;

    private String S3_FILE_LOCATION;

    @PostConstruct
    private void init() {
        S3_FILE_LOCATION = String.format("https://%s.s3.amazonaws.com/", s3BucketName);
    }

    private final FileStorageRepository fileStorageRepository;
    private final EventService eventService;
    private final FileRepository fileRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public Mono<UploadedFileResponseDTO> uploadUserFileToStorage(FilePart filePart, Mono<Authentication> authMono) {
        log.info("IN FileStorageServiceImpl uploadUserFileToStorage:");
        return authMono
                .flatMap(auth ->
                        extractUserId(auth)
                                .map(userId -> new UserContext(userId, auth.getAuthorities())))
                .flatMap(userContext -> {
                    long userId = userContext.userId();
                    String filename = filePart.filename();
                    String location = S3_FILE_LOCATION + filename;

                    File file = File.builder()
                            .location(location)
                            .build();

                    return fileRepository.save(file)
                            .flatMap(savedFile -> {
                                Event event = Event.builder()
                                        .userId(userId)
                                        .fileId(savedFile.getId())
                                        .build();

                                return eventRepository.save(event);
                            })
                            .then(fileStorageRepository.uploadUserFileToStorage(filePart))
                            .doOnSuccess(unused -> log.info(INFO_FILE_UPLOADED_SUCCESSFULLY_WITH_FILENAME_AND_USER_ID, filename, userId))
                            .doOnError(error -> log.error(ERR_UPLOADING_FILE_WITH_FILENAME_AND_USER_ID, filename, userId, error.getMessage()));
                });
    }

    private Mono<Long> extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomPrincipal customPrincipal) {
            return Mono.just(customPrincipal.getId());
        }
        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_INVALID_AUTHENTICATION));
    }

    private record UserContext(
            Long userId,
            Collection<? extends GrantedAuthority> authorities) {
    }

    @Override
    public Mono<ResponseEntity<Resource>> downloadFileFromStorageByFileNameAndAuth(String fileName, Mono<Authentication> authMono) {
        log.info("IN FileStorageServiceImpl downloadFileFromStorageByFileName: {}", fileName);
        return authMono
                .flatMap(auth -> extractUserId(auth).map(userId -> new UserContext(userId, auth.getAuthorities())))
                .defaultIfEmpty(new UserContext(null, Collections.emptyList()))
                .flatMap(userContext -> checkUserAccessToFile(fileName, userContext.userId, userContext.authorities))
                .flatMap(hasAccess -> {
                    if (!hasAccess) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_ACCESS_DENIED));
                    }
                    return downloadFile(fileName);
                })
                .onErrorMap(this::handleDownloadError)
                .doOnSuccess(unused -> log.info(INFO_FILE_DOWNLOADED_SUCCESSFULLY_WITH_FILENAME, fileName))
                .doOnError(error -> log.error(ERR_DOWNLOADING_FILE_WITH_FILENAME, fileName, error.getMessage()));
    }

    private Mono<Boolean> checkUserAccessToFile(String fileName, Long userId, Collection<? extends GrantedAuthority> authorities) {
        if (authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + UserRole.USER.name()))) {
            return eventService.getEventByFileNameAndUserId(fileName, userId)
                    .map(e -> true)
                    .defaultIfEmpty(false);
        }
        return Mono.just(true);
    }

    private Mono<ResponseEntity<Resource>> downloadFile(String fileName) {
        return fileStorageRepository.downloadFileFromStorage(fileName)
                .map(responseEntity -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(responseEntity.getBody()));
    }

    private Throwable handleDownloadError(Throwable error) {
        if (error instanceof NoSuchKeyException) {
            log.error(ERR_FILE_NOT_FOUND_IN_S_3, error.getMessage(), error);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_FILE_NOT_FOUND_IN_S_3, error);
        }
        return error;
    }
}
