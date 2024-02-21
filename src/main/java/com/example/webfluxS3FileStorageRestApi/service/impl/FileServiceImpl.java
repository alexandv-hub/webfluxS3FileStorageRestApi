package com.example.webfluxS3FileStorageRestApi.service.impl;

import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.repository.EventRepository;
import com.example.webfluxS3FileStorageRestApi.repository.FileRepository;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.ERR_ACCESS_DENIED;
import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.Files.*;
import static com.example.webfluxS3FileStorageRestApi.messages.InfoMessages.Files.*;
import static com.example.webfluxS3FileStorageRestApi.security.SecurityUtils.isAdminOrModerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final EventRepository eventRepository;

    @Override
    public Mono<File> getFileByIdAndAuth(Long id, Mono<Authentication> authMono) {
        log.info("IN FileServiceImpl getFileByIdAndAuth: {}", id);
        return authMono
                .flatMap(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMap(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return fileRepository.findActiveById(id);
                                } else {
                                    return existsByIdAndUserId(id, principal.getId())
                                            .flatMap(exists -> exists ?
                                                    fileRepository.findActiveById(id) :
                                                    Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_ACCESS_DENIED)));
                                }
                            });
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(ERR_FILE_WITH_ID_NOT_FOUND, id))))
                .doOnSuccess(unused -> log.info(INFO_FILE_FOUND_SUCCESSFULLY_WITH_ID, id))
                .doOnError(error -> log.error(ERR_FIND_FILE_WITH_ID, id, error.getMessage()));
    }

    @Override
    public Mono<Boolean> existsByIdAndUserId(Long fileId, Long userId) {
        log.info("IN FileServiceImpl existsByIdAndUserId: {}, {}", fileId, userId);
        return eventRepository.findActiveByFileIdAndUserId(fileId, userId)
                .map(e -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<File> getAllFilesByAuth(Mono<Authentication> authMono) {
        log.info("IN FileServiceImpl getAllFilesByAuth:");
        return authMono.flatMapMany(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMapMany(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return fileRepository.findAllActive();
                                } else {
                                    return fileRepository.findAllActiveByUserId(principal.getId());
                                }
                            });
                })
                .doOnComplete(() -> log.info(INFO_FIND_ALL_FILES_FINISHED_SUCCESSFULLY))
                .doOnError(error -> log.error(ERR_FIND_ALL_FILES, error.getMessage()));
    }

    @Override
    public Flux<File> getFilesByUserId(Long userId) {
        log.info("IN FileServiceImpl getFilesByUserId: {}", userId);
        return fileRepository.findAllActiveByUserId(userId)
                .doOnComplete(() -> log.info(INFO_FIND_ALL_FILES_BY_USER_ID_FINISHED_SUCCESSFULLY))
                .doOnError(error -> log.error(ERR_FIND_ALL_FILES_BY_USER_ID, error.getMessage()));
    }

    @Override
    public Mono<File> updateFileById(Long id, File file) {
        log.info("IN FileServiceImpl updateFileById: {}", file);
        return fileRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(ERR_FILE_WITH_ID_NOT_FOUND, id))))
                .flatMap(foundFile -> {
                    foundFile.setLocation(file.getLocation());
                    return fileRepository.save(foundFile);
                })
                .doOnSuccess(aVoid -> log.info(INFO_FILE_UPDATED_SUCCESSFULLY_WITH_ID, id))
                .doOnError(error -> log.error(ERR_UPDATING_FILE_WITH_ID, id, error.getMessage()));
    }

    @Override
    public Mono<Void> deleteFileById(Long id) {
        log.info("IN FileServiceImpl deleteFileById: '{}'", id);
        return fileRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(ERR_FILE_WITH_ID_NOT_FOUND, id))))
                .flatMap(file -> {
                    log.info(INFO_DELETING_FILE_WITH_ID, id);
                    return fileRepository.deleteActiveById(id);
                })
                .then()
                .doOnSuccess(aVoid -> log.info(INFO_FILE_DELETED_SUCCESSFULLY_WITH_ID, id))
                .doOnError(error -> log.error(ERR_DELETING_FILE_WITH_ID, id, error.getMessage()));
    }

    @Override
    public Mono<Integer> deleteAllFilesByUserId(Long userId) {
        log.info("IN FileServiceImpl deleteAllFilesByUserId: {}", userId);
        return fileRepository.deleteAllActiveByUserId(userId)
                .doOnTerminate(() -> log.info(INFO_ALL_FILES_DELETED_SUCCESSFULLY_WITH_USER_ID, userId))
                .doOnError(error -> log.error(ERR_DELETING_ALL_FILES_WITH_USER_ID, userId, error.getMessage()));
    }

    @Override
    public Mono<Integer> deleteAllFiles() {
        log.info("IN FileServiceImpl deleteAllFiles");
        return fileRepository.deleteAllActive()
                .doOnTerminate(() -> log.info(INFO_ALL_FILES_DELETED_SUCCESSFULLY))
                .doOnError(error -> log.error(ERR_DELETING_ALL_FILES, error.getMessage()));
    }
}
