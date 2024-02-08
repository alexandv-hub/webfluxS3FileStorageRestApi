package com.example.webfluxS3FileStorageRestApi.service.impl;

import com.example.webfluxS3FileStorageRestApi.dto.FileDTO;
import com.example.webfluxS3FileStorageRestApi.mapper.FileMapper;
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
    private final FileMapper fileMapper;

    @Override
    public Mono<FileDTO> getFileByIdAndAuth(Long fileId, Mono<Authentication> authMono) {
        log.info("IN FileServiceImpl getFileByIdAndAuth: {}", fileId);
        return authMono
                .flatMap(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMap(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return fileRepository.findActiveById(fileId);
                                } else {
                                    return existsByIdAndUserId(fileId, principal.getId())
                                            .flatMap(exists -> exists ?
                                                    fileRepository.findActiveById(fileId) :
                                                    Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_ACCESS_DENIED)));
                                }
                            })
                            .map(fileMapper::map);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getFileWithIdNotFoundStr(fileId))))
                .doOnSuccess(unused -> log.info(INFO_FILE_FOUND_SUCCESSFULLY_WITH_ID, fileId))
                .doOnError(error -> log.error(ERR_FIND_FILE_WITH_ID, fileId, error.getMessage()));
    }

    @Override
    public Mono<Boolean> existsByIdAndUserId(Long fileId, Long userId) {
        log.info("IN FileServiceImpl existsByIdAndUserId: {}, {}", fileId, userId);
        return eventRepository.findActiveByFileIdAndUserId(fileId, userId)
                .map(e -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<FileDTO> getAllFiles(Mono<Authentication> authMono) {
        log.info("IN FileServiceImpl getAllFiles");
        return authMono.flatMapMany(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMapMany(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return fileRepository.findAllActive();
                                } else {
                                    return fileRepository.findAllActiveByUserId(principal.getId());
                                }
                            })
                            .map(fileMapper::map);
                })
                .doOnComplete(() -> log.info(INFO_FIND_ALL_FILES_FINISHED_SUCCESSFULLY))
                .doOnError(error -> log.error(ERR_FIND_ALL_FILES, error.getMessage()));
    }

    @Override
    public Mono<FileDTO> updateFile(FileDTO fileDTO) {
        log.info("IN FileServiceImpl updateFile: {}", fileDTO);
        Long fileDTOId = fileDTO.getId();
        return fileRepository.findActiveById(fileDTOId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getFileWithIdNotFoundStr(fileDTOId))))
                .flatMap(file -> {
                    file.setLocation(fileDTO.getLocation());
                    return fileRepository.save(file)
                            .map(fileMapper::map);
                })
                .doOnSuccess(aVoid -> log.info(INFO_FILE_UPDATED_SUCCESSFULLY_WITH_ID, fileDTOId))
                .doOnError(error -> log.error(ERR_UPDATING_FILE_WITH_ID, fileDTOId, error.getMessage()));
    }

    @Override
    public Mono<Void> deleteFileById(Long id) {
        log.info("IN FileServiceImpl deleteFileById: '{}'", id);
        return fileRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getFileWithIdNotFoundStr(id))))
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
                .doOnTerminate(() -> log.info(INFO_ALL_FILES_DELETED_SUCCESSFULLY))
                .doOnError(error -> log.error(ERR_DELETING_ALL_FILES, error.getMessage()));

    }

    @Override
    public Mono<Integer> deleteAllFiles() {
        log.info("IN FileServiceImpl deleteAllFiles");
        return fileRepository.deleteAllActive()
                .doOnTerminate(() -> log.info(INFO_ALL_FILES_DELETED_SUCCESSFULLY1))
                .doOnError(error -> log.error(ERR_DELETING_ALL_FILES1, error.getMessage()));
    }

    private static String getFileWithIdNotFoundStr(Long id) {
        return "File with ID = '" + id + "' not found";
    }
}
