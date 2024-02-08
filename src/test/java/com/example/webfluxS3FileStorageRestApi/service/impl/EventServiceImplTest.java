package com.example.webfluxS3FileStorageRestApi.service.impl;

import com.example.webfluxS3FileStorageRestApi.dto.EventDTO;
import com.example.webfluxS3FileStorageRestApi.mapper.EventMapper;
import com.example.webfluxS3FileStorageRestApi.model.Event;
import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import com.example.webfluxS3FileStorageRestApi.repository.EventRepository;
import com.example.webfluxS3FileStorageRestApi.repository.FileRepository;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private EventServiceImpl eventService;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;


    @Test
    void getEventByIdAndAuth_AsAdminOrModerator_Success() {
        Long eventId = 1L;
        Long userId = 1L;

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        Event event = new Event();
        when(eventRepository.findActiveById(eventId)).thenReturn(Mono.just(event));
        EventDTO eventDTO = EventDTO.builder().build();
        when(eventMapper.map(event)).thenReturn(eventDTO);

        StepVerifier.create(eventService.getEventByIdAndAuth(eventId, Mono.just(authentication)))
                .expectNext(eventDTO)
                .verifyComplete();

        verify(eventRepository).findActiveById(eventId);
        verify(eventRepository, never()).findActiveByFileIdAndUserId(eventId, userId);
    }

    @Test
    void getEventByIdAndAuth_AsUser_NotExistsOrForbidden() {
        Long eventId = 1L;
        Long userId = 1L;

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(eventRepository.findActiveByIdAndUserId(eventId, userId)).thenReturn(Mono.empty());

        StepVerifier.create(eventService.getEventByIdAndAuth(eventId, Mono.just(authentication)))
                .expectError(ResponseStatusException.class)
                .verify();

        verify(eventRepository).findActiveByIdAndUserId(eventId, userId);
        verify(eventRepository, never()).findActiveById(eventId);
    }

    @Test
    void getEventByIdAndAuth_AsUser_Success() {
        Long eventId = 1L;
        Long userId = 1L;

        Event event = new Event();
        EventDTO eventDTO = EventDTO.builder().build();

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(eventRepository.findActiveByIdAndUserId(eventId, userId)).thenReturn(Mono.just(event));
        when(eventRepository.findActiveById(eventId)).thenReturn(Mono.just(event));
        when(eventMapper.map(event)).thenReturn(eventDTO);

        StepVerifier.create(eventService.getEventByIdAndAuth(eventId, Mono.just(authentication)))
                .expectNext(eventDTO)
                .verifyComplete();
        
        verify(eventRepository).findActiveByIdAndUserId(eventId, userId);
        verify(eventRepository).findActiveById(eventId);
    }



    @Test
    void getAllEvents_AsAdminOrModerator_ReturnsAllEvents() {
        Long userId = 1L;
        Long file1Id = 1L;
        Long file2Id = 2L;
        Long event1Id = 1L;
        Long event2Id = 2L;

        File file1 = File.builder()
                .id(file1Id)
                .build();
        File file2 = File.builder()
                .id(file2Id)
                .build();

        Event event1 = Event.builder()
                .id(event1Id)
                .userId(userId)
                .fileId(file1Id)
                .build();
        Event event2 = Event.builder()
                .id(event2Id)
                .userId(userId)
                .fileId(file2Id)
                .build();

        EventDTO eventDTO1 = EventDTO.builder()
                .id(event1Id)
                .userId(userId)
                .fileId(file1Id)
                .build();
        EventDTO eventDTO2 = EventDTO.builder()
                .id(event2Id)
                .userId(userId)
                .fileId(file2Id)
                .build();

        List<Event> events = List.of(event1, event2);
        List<EventDTO> eventDTOs = List.of(eventDTO1, eventDTO2);

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(eventRepository.findAllActive()).thenReturn(Flux.fromIterable(events));
        when(fileRepository.findActiveById(file1Id)).thenReturn(Mono.just(file1));
        when(fileRepository.findActiveById(file2Id)).thenReturn(Mono.just(file2));
        when(eventMapper.map(event1, file1)).thenReturn(eventDTO1);
        when(eventMapper.map(event2, file2)).thenReturn(eventDTO2);

        StepVerifier.create(eventService.getAllEvents(Mono.just(authentication)))
                .expectNextSequence(eventDTOs)
                .verifyComplete();

        verify(eventRepository).findAllActive();
        verify(eventRepository, never()).findAllActiveByUserId(userId);
    }

    @Test
    void getAllEvents_AsUser_ReturnsUserEvents() {
        Long userId = 1L;

        Long file1Id = 1L;
        Long file2Id = 2L;

        Long event1Id = 3L;
        Long event2Id = 4L;

        File file1 = File.builder()
                .id(file1Id)
                .build();
        File file2 = File.builder()
                .id(file2Id)
                .build();

        Event event1 = Event.builder()
                .id(event1Id)
                .userId(userId)
                .fileId(file1Id)
                .build();
        Event event2 = Event.builder()
                .id(event2Id)
                .userId(userId)
                .fileId(file2Id)
                .build();

        EventDTO eventDTO1 = EventDTO.builder()
                .id(event1Id)
                .userId(userId)
                .fileId(file1Id)
                .build();
        EventDTO eventDTO2 = EventDTO.builder()
                .id(event2Id)
                .userId(userId)
                .fileId(file2Id)
                .build();

        List<Event> userEvents = List.of(event1, event2);
        List<EventDTO> userEventDTOs = List.of(eventDTO1, eventDTO2);

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(eventRepository.findAllActiveByUserId(userId)).thenReturn(Flux.fromIterable(userEvents));
        when(fileRepository.findActiveById(file1Id)).thenReturn(Mono.just(file1));
        when(fileRepository.findActiveById(file2Id)).thenReturn(Mono.just(file2));
        when(eventMapper.map(event1, file1)).thenReturn(eventDTO1);
        when(eventMapper.map(event2, file2)).thenReturn(eventDTO2);

        StepVerifier.create(eventService.getAllEvents(Mono.just(authentication)))
                .expectNextSequence(userEventDTOs)
                .verifyComplete();
        
        verify(eventRepository).findAllActiveByUserId(userId);
        verify(eventRepository, never()).findAllActive();
    }



    @Test
    void updateEvent_EventExists_UpdatesSuccessfully() {
        Long eventId = 1L;

        Long originalUserId = 1L;
        Long updatedUserId = 2L;

        Long originalFileId = 3L;
        Long updatedFileId = 4L;

        Event event = Event.builder()
                .id(eventId)
                .userId(originalUserId)
                .fileId(originalFileId)
                .user(new UserEntity())
                .file(new File())
                .build();

        EventDTO updatedEventDTO = EventDTO.builder()
                .id(eventId)
                .userId(updatedUserId)
                .fileId(updatedFileId)
                .build();

        when(eventRepository.findActiveById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(event));
        when(eventMapper.map(any(Event.class))).thenReturn(updatedEventDTO);

        StepVerifier.create(eventService.updateEvent(updatedEventDTO))
                .expectNext(updatedEventDTO)
                .verifyComplete();

        verify(eventRepository).save(eventCaptor.capture());
        Event savedEvent = eventCaptor.getValue();
        assertEquals(updatedUserId, savedEvent.getUserId());
        assertEquals(updatedFileId, savedEvent.getFileId());
    }

    @Test
    void updateEvent_EventDoesNotExist_ThrowsException() {
        Long eventId = 1L;
        Long userId = 2L;
        Long fileId = 3L;

        EventDTO eventDTO = EventDTO.builder()
                .id(eventId)
                .userId(userId)
                .fileId(fileId)
                .build();

        when(eventRepository.findActiveById(eventId)).thenReturn(Mono.empty());

        StepVerifier.create(eventService.updateEvent(eventDTO))
                .expectError(ResponseStatusException.class)
                .verify();
    }



    @Test
    void deleteEventById_WhenEventExists_CompletesSuccessfully() {
        Long eventId = 1L;
        when(eventRepository.findActiveById(eventId)).thenReturn(Mono.just(new Event()));
        when(eventRepository.deleteActiveById(eventId)).thenReturn(Mono.empty());

        StepVerifier.create(eventService.deleteEventById(eventId))
                .verifyComplete();

        verify(eventRepository).findActiveById(eventId);
        verify(eventRepository).deleteActiveById(eventId);
    }

    @Test
    void deleteEventById_WhenEventDoesNotExist_ThrowsNotFoundException() {
        Long eventId = 2L;
        when(eventRepository.findActiveById(eventId)).thenReturn(Mono.empty());

        StepVerifier.create(eventService.deleteEventById(eventId))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode().equals(HttpStatus.NOT_FOUND))
                .verify();

        verify(eventRepository).findActiveById(eventId);
        verify(eventRepository, never()).deleteActiveById(eventId);
    }



    @Test
    void deleteAllEventsByUserId_SuccessfulDeletion_ReturnsCount() {
        Long userId = 1L;
        doReturn(Mono.just(3)).when(eventRepository).deleteAllActiveByUserId(userId);

        StepVerifier.create(eventService.deleteAllEventsByUserId(userId))
                .expectNext(3)
                .verifyComplete();

        verify(eventRepository).deleteAllActiveByUserId(userId);
    }

    @Test
    void deleteAllEventsByUserId_ErrorOccurs_PropagatesError() {
        Long userId = 2L;
        RuntimeException exception = new RuntimeException("Database error");
        when(eventRepository.deleteAllActiveByUserId(userId)).thenReturn(Mono.error(exception));

        StepVerifier.create(eventService.deleteAllEventsByUserId(userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Database error"))
                .verify();

        verify(eventRepository).deleteAllActiveByUserId(userId);
    }



    @Test
    void deleteAllEvents_SuccessfulDeletion_ReportsCompletion() {
        doReturn(Mono.just(5)).when(eventRepository).deleteAllActive();

        StepVerifier.create(eventService.deleteAllEvents())
                .expectNext(5)
                .verifyComplete();

        verify(eventRepository).deleteAllActive();
    }

    @Test
    void deleteAllEvents_ErrorOccurs_PropagatesError() {
        RuntimeException exception = new RuntimeException("Database error");
        when(eventRepository.deleteAllActive()).thenReturn(Mono.error(exception));

        StepVerifier.create(eventService.deleteAllEvents())
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                                                 "Database error".equals(throwable.getMessage()))
                .verify();

        verify(eventRepository).deleteAllActive();
    }

}
