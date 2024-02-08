package com.example.webfluxS3FileStorageRestApi.service.impl;

import com.example.webfluxS3FileStorageRestApi.dto.EventDTO;
import com.example.webfluxS3FileStorageRestApi.mapper.EventMapper;
import com.example.webfluxS3FileStorageRestApi.repository.EventRepository;
import com.example.webfluxS3FileStorageRestApi.repository.FileRepository;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.ERR_ACCESS_DENIED;
import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.Events.*;
import static com.example.webfluxS3FileStorageRestApi.messages.InfoMessages.Events.*;
import static com.example.webfluxS3FileStorageRestApi.security.SecurityUtils.isAdminOrModerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final FileRepository fileRepository;
    private final EventMapper eventMapper;

    @Override
    public Mono<EventDTO> getEventByIdAndAuth(Long id, Mono<Authentication> authMono) {
        log.info("IN EventServiceImpl getEventByIdAndAuth: {}", id);
        return authMono
                .flatMap(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMap(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return eventRepository.findActiveById(id);
                                } else {
                                    return existsByIdAndUserId(id, principal.getId())
                                            .flatMap(exists -> exists ?
                                                    eventRepository.findActiveById(id) :
                                                    Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_ACCESS_DENIED)));
                                }
                            })
                            .flatMap(event -> fileRepository.findActiveById(event.getFileId())
                                    .map(file -> eventMapper.map(event, file)));
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getEventWithIdNotFoundStr(id))))
                .doOnSuccess(unused -> log.info(INFO_EVENT_FOUND_SUCCESSFULLY_WITH_ID, id))
                .doOnError(error -> log.error(ERR_FIND_EVENT_WITH_ID, id, error.getMessage()));
    }

    @Override
    public Mono<Boolean> existsByIdAndUserId(Long eventId, Long userId) {
        log.info("IN EventServiceImpl existsByIdAndUserId: {}, {}", eventId, userId);
        return eventRepository.findActiveByIdAndUserId(eventId, userId)
                .map(e -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<EventDTO> getAllEvents(Mono<Authentication> authMono) {
        log.info("IN EventServiceImpl getAllEvents");
        return authMono.flatMapMany(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMapMany(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return eventRepository.findAllActive();
                                } else {
                                    return eventRepository.findAllActiveByUserId(principal.getId());
                                }
                            })
                            .flatMap(event -> fileRepository.findActiveById(event.getFileId())
                                    .map(file -> eventMapper.map(event, file)));
                })
                .doOnComplete(() -> log.info(INFO_FIND_ALL_EVENTS_FINISHED_SUCCESSFULLY))
                .doOnError(error -> log.error(ERR_FIND_ALL_EVENTS, error.getMessage()));
    }

    @Override
    public Flux<EventDTO> getEventsByUserId(Long userId) {
        log.info("IN EventServiceImpl getEventsByUserId: {}", userId);
        return eventRepository.findAllActiveByUserId(userId)
                .flatMap(event -> fileRepository.findActiveById(event.getFileId())
                        .map(file -> eventMapper.map(event, file)))
                .doOnComplete(() -> log.info(INFO_FIND_ALL_EVENTS_BY_USER_ID_FINISHED_SUCCESSFULLY))
                .doOnError(error -> log.error(ERR_FIND_ALL_EVENTS_BY_USER_ID, error.getMessage()));
    }

    @Override
    public Mono<EventDTO> getEventByFileNameAndUserId(String fileName, Long userId) {
        log.info("IN EventServiceImpl getEventByFileNameAndUserId: {}, {}", fileName, userId);
        return fileRepository.getIdByFileName(fileName)
                .flatMap(fileId ->
                        eventRepository.findActiveByFileIdAndUserId(userId, fileId)
                )
                .map(eventMapper::map)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_EVENT_NOT_FOUND)));
    }

    @Override
    public Mono<EventDTO> updateEvent(EventDTO eventDTO) {
        log.info("IN EventServiceImpl updateEvent: {}", eventDTO);
        Long eventDTOId = eventDTO.getId();
        return eventRepository.findActiveById(eventDTOId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getEventWithIdNotFoundStr(eventDTOId))))
                .flatMap(event -> {
                    event.setUserId(eventDTO.getUserId());
                    event.setFileId(eventDTO.getFileId());
                    return eventRepository.save(event);
                })
                .map(eventMapper::map)
                .doOnSuccess(aVoid -> log.info(INFO_EVENT_UPDATED_SUCCESSFULLY_WITH_ID, eventDTOId))
                .doOnError(error -> log.error(ERR_UPDATING_EVENT_WITH_ID, eventDTOId, error.getMessage()));
    }

    @Override
    public Mono<Void> deleteEventById(Long id) {
        log.info("IN EventServiceImpl deleteEventById: {}", id);
        return eventRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getEventWithIdNotFoundStr(id))))
                .flatMap(file -> {
                    log.info(INFO_DELETING_EVENT_WITH_ID, id);
                    return eventRepository.deleteActiveById(id);
                })
                .then()
                .doOnSuccess(aVoid -> log.info(INFO_EVENT_DELETED_SUCCESSFULLY_WITH_ID, id))
                .doOnError(error -> log.error(ERR_DELETING_EVENT_WITH_ID, id, error.getMessage()));
    }

    @Override
    public Mono<Integer> deleteAllEventsByUserId(Long userId) {
        log.info("IN EventServiceImpl deleteAllEventsByUserId: {}", userId);
        return eventRepository.deleteAllActiveByUserId(userId)
                .doOnTerminate(() -> log.info(INFO_EVENTS_DELETED_SUCCESSFULLY_WITH_USER_ID, userId))
                .doOnError(error -> log.error(ERR_DELETING_ALL_EVENTS_WITH_USER_ID, error.getMessage()));
    }

    @Override
    public Mono<Integer> deleteAllEvents() {
        log.info("IN EventServiceImpl deleteAllEvents");
        return eventRepository.deleteAllActive()
                .doOnTerminate(() -> log.info(INFO_ALL_EVENTS_DELETED_SUCCESSFULLY))
                .doOnError(error -> log.error(ERR_DELETING_ALL_EVENTS, error.getMessage()));
    }

    private String getEventWithIdNotFoundStr(Long id) {
        return "Event with ID = '" + id + "' not found";
    }
}
