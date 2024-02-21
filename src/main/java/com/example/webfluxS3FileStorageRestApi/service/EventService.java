package com.example.webfluxS3FileStorageRestApi.service;

import com.example.webfluxS3FileStorageRestApi.dto.EventBasicDTO;
import com.example.webfluxS3FileStorageRestApi.dto.EventDTO;
import com.example.webfluxS3FileStorageRestApi.dto.EventUpdateRequestDTO;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventService {

    Mono<EventDTO> getEventByIdAndAuth(Long id, Mono<Authentication> authMono);

    Mono<Boolean> existsByIdAndUserId(Long fileId, Long userId);

    Flux<EventDTO> getAllEventsByAuth(Mono<Authentication> authMono);

    Flux<EventDTO> getEventsByUserId(Long userId);

    Mono<EventDTO> getEventByFileNameAndUserId(String fileName, Long userId);

    Mono<EventBasicDTO> updateEventById(Long id, EventUpdateRequestDTO eventUpdateRequestDTO);

    Mono<Void> deleteEventById(Long id);

    Mono<Integer> deleteAllEventsByUserId(Long userId);

    Mono<Integer> deleteAllEvents();
}
