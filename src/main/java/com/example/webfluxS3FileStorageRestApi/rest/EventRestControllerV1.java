package com.example.webfluxS3FileStorageRestApi.rest;

import com.example.webfluxS3FileStorageRestApi.dto.EventDTO;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;

import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
@Tag(name = "Event", description = "Operations related to events")
public class EventRestControllerV1 {

    private final EventService eventService;

    @GetMapping("/{id}")
    @Operation(summary = "Find an event by ID", description = "Finds an event with the specified ID")
    public Mono<EntityModel<EventDTO>> getEventById(@PathVariable Long id, Mono<Authentication> authMono) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (CustomPrincipal) securityContext.getAuthentication().getPrincipal())
                .flatMap(customPrincipal ->
                        eventService.getEventByIdAndAuth(id, authMono)
                                .flatMap(eventDTO -> buildEntityModelWithLinks(eventDTO, authMono)));
    }

    @GetMapping("/")
    @Operation(summary = "Find all events or events by user ID", description = "Finds all events or events by user ID")
    public Flux<EntityModel<EventDTO>> getAllEventsByAuth(Mono<Authentication> authMono) {
        return eventService.getAllEvents(authMono)
                .flatMap(eventDTO -> buildEntityModelWithLinks(eventDTO, authMono));
    }

    @GetMapping("/by-user-id")
    @Operation(summary = "Find all events by user ID", description = "Finds all events by user ID")
    public Flux<EntityModel<EventDTO>> getAllEventsByUserId(@RequestParam Long userId, Mono<Authentication> authMono) {
        return eventService.getEventsByUserId(userId)
                .flatMap(eventDTO -> buildEntityModelWithLinks(eventDTO, authMono));
    }

    @PutMapping(path = "/")
    @Operation(summary = "Update an event", description = "Updates an event")
    public Mono<EventDTO> updateEvent(@RequestBody EventDTO eventDTO) {
        return eventService.updateEvent(eventDTO);
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Delete an event by ID", description = "Deletes an event with the specified ID")
    public Mono<Void> deleteEventById(@PathVariable Long eventId) {
        return eventService.deleteEventById(eventId);
    }

    @DeleteMapping("/")
    @Operation(summary = "Delete all events by user ID", description = "Deletes all events by user ID")
    public Mono<Integer> deleteAllEventsByUserId(@RequestParam Long userId) {
        return eventService.deleteAllEventsByUserId(userId);
    }

    @DeleteMapping("/all")
    @Operation(summary = "Delete all events!!!", description = "Deletes all events!!!")
    public Mono<Integer> deleteAllEvents() {
        return eventService.deleteAllEvents();
    }

    private Mono<EntityModel<EventDTO>> buildEntityModelWithLinks(EventDTO eventDTO, Mono<Authentication> authMono) {
        Mono<Link> selfLinkMono = linkTo(methodOn(
                FileRestControllerV1.class).getFileById(eventDTO.getFileId(), authMono)).withSelfRel().toMono();

        String fileName = Paths.get(eventDTO.getFileDTO().getLocation()).getFileName().toString();
        Mono<Link> downloadLinkMono = linkTo(methodOn(
                FileStorageRestControllerV1.class).downloadFileByName(fileName, authMono)).withRel("download").toMono();

        return Mono.zip(selfLinkMono, downloadLinkMono)
                .map(links -> EntityModel.of(eventDTO, links.getT1(), links.getT2()));
    }
}
