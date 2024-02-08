package com.example.webfluxS3FileStorageRestApi.service;

import com.example.webfluxS3FileStorageRestApi.dto.EventDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventService {

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Mono<EventDTO> getEventByIdAndAuth(Long id, Mono<Authentication> authMono);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Mono<Boolean> existsByIdAndUserId(Long fileId, Long userId);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Flux<EventDTO> getAllEvents(Mono<Authentication> authMono);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    Flux<EventDTO> getEventsByUserId(Long userId);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Mono<EventDTO> getEventByFileNameAndUserId(String fileName, Long userId);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    Mono<EventDTO> updateEvent(EventDTO eventDTO);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    Mono<Void> deleteEventById(Long id);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    Mono<Integer> deleteAllEventsByUserId(Long userId);

    @PreAuthorize("hasAnyRole('ADMIN')")
    Mono<Integer> deleteAllEvents();
}
