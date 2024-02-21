package com.example.webfluxS3FileStorageRestApi.integration.rest.mock;

import com.example.webfluxS3FileStorageRestApi.dto.EventBasicDTO;
import com.example.webfluxS3FileStorageRestApi.dto.EventDTO;
import com.example.webfluxS3FileStorageRestApi.dto.EventUpdateRequestDTO;
import com.example.webfluxS3FileStorageRestApi.integration.config.ErrorHandlerConfig;
import com.example.webfluxS3FileStorageRestApi.integration.config.TestWebSecurityConfig;
import com.example.webfluxS3FileStorageRestApi.mapper.EventMapper;
import com.example.webfluxS3FileStorageRestApi.mapper.UserMapper;
import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.rest.EventRestControllerV1;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.security.SecurityService;
import com.example.webfluxS3FileStorageRestApi.service.EventService;
import com.example.webfluxS3FileStorageRestApi.service.FileService;
import com.example.webfluxS3FileStorageRestApi.service.FileStorageService;
import com.example.webfluxS3FileStorageRestApi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.ERR_ACCESS_DENIED;
import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.Events.ERR_EVENT_WITH_ID_NOT_FOUND;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


@ActiveProfiles("test")
@WebFluxTest(controllers = EventRestControllerV1.class)
@Import({ErrorHandlerConfig.class, TestWebSecurityConfig.class})
public class EventRestControllerV1MockIT {

    @MockBean
    private SecurityService securityService;
    @MockBean
    private UserService userService;
    @MockBean
    private EventMapper eventMapper;
    @MockBean
    private UserMapper userMapper;
    @MockBean
    private EventService eventService;
    @MockBean
    private FileService fileService;
    @MockBean
    private FileStorageService fileStorageService;

    @Autowired
    private WebTestClient webTestClient;

    private Authentication authentication;

    @BeforeEach
    public void initAuthentication_WithRoleAdmin() {
        Long userId = 1L;
        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "testUser");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authentication = new UsernamePasswordAuthenticationToken(
                customPrincipal, null, authorities);
    }


    @Test
    void getEventById_ReturnsEvent() {
        Long eventId = 1L;
        Long userId = 2L;
        Long fileId = 3L;
        String fileName = "testFile.txt";
        String someFileLocation = "/prefix/" + fileName;
        File file = File.builder()
                .id(fileId)
                .location(someFileLocation)
                .build();
        EventDTO eventDTO = EventDTO.builder()
                .id(eventId)
                .userId(userId)
                .fileId(fileId)
                .file(file)
                .build();

        when(eventService.getEventByIdAndAuth(eq(eventId), any())).thenReturn(Mono.just(eventDTO));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/events/" + eventId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(eventId)
                .jsonPath("$.user_id").isEqualTo(userId)
                .jsonPath("$.file_id").isEqualTo(fileId)
                .jsonPath("$.file.id").isEqualTo(fileId)
                .jsonPath("$.file.location").isEqualTo(someFileLocation)
                .jsonPath("$.links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/" + fileId)
                .jsonPath("$.links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/" + fileName);
    }

    @Test
    void getEventById_WhenEventNotFound_ReturnsNotFound() {
        Long eventId = 2L;
        String uri = "/api/v1/events/" + eventId;
        String errorMessage = String.format(ERR_EVENT_WITH_ID_NOT_FOUND, eventId);

        when(eventService.getEventByIdAndAuth(eq(eventId), any()))
                .thenReturn(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        errorMessage)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri(uri)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo(uri)
                .jsonPath("$.requestId").isNotEmpty()
                .jsonPath("$.error").isEqualTo("NOT_FOUND")
                .jsonPath("$.message").isEqualTo(errorMessage)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void getEventById_WhenUserForbidden_ReturnsForbidden_HandlesResponseStatusException() {
        Long eventId = 3L;
        String uri = "/api/v1/events/" + eventId ;
        String errorMessage = ERR_ACCESS_DENIED;

        when(eventService.getEventByIdAndAuth(eq(eventId), any()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, errorMessage)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri(uri)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.path").isEqualTo(uri)
                .jsonPath("$.requestId").isNotEmpty()
                .jsonPath("$.error").isEqualTo("FORBIDDEN")
                .jsonPath("$.message").isEqualTo(errorMessage)
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.timestamp").isNotEmpty();
    }



    @Test
    void getAllEvents_ReturnsEvents() {
        Long event1Id = 1L;
        Long user1Id = 2L;
        Long file1Id = 3L;
        String file1Name = "testFile1.txt";
        String someFile1Location = "/prefix/" + file1Name;
        File file1 = File.builder()
                .id(file1Id)
                .location(someFile1Location)
                .build();
        EventDTO eventDTO1 = EventDTO.builder()
                .id(event1Id)
                .userId(user1Id)
                .fileId(file1Id)
                .file(file1)
                .build();

        Long event2Id = 4L;
        Long user2Id = 5L;
        Long file2Id = 6L;
        String file2Name = "testFile2.txt";
        String someFile2Location = "/prefix/" + file2Name;
        File file2 = File.builder()
                .id(file2Id)
                .location(someFile2Location)
                .build();
        EventDTO eventDTO2 = EventDTO.builder()
                .id(event2Id)
                .userId(user2Id)
                .fileId(file2Id)
                .file(file2)
                .build();

        List<EventDTO> events = List.of(eventDTO1, eventDTO2);

        when(eventService.getAllEventsByAuth(any()))
                .thenReturn(Flux.fromIterable(events));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/events/")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").value(hasSize(2))
                .jsonPath("$.[0].id").isEqualTo(event1Id)
                .jsonPath("$.[0].user_id").isEqualTo(user1Id)
                .jsonPath("$.[0].file_id").isEqualTo(file1Id)
                .jsonPath("$.[0].links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/" + file1Id)
                .jsonPath("$.[0].links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/" + file1Name)
                .jsonPath("$.[1].id").isEqualTo(event2Id)
                .jsonPath("$.[1].user_id").isEqualTo(user2Id)
                .jsonPath("$.[1].file_id").isEqualTo(file2Id)
                .jsonPath("$.[1].links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/" + file2Id)
                .jsonPath("$.[1].links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/" + file2Name);
    }

    @Test
    void getAllEvents_ReturnsEmptyArray() {
        when(eventService.getAllEventsByAuth(any()))
                .thenReturn(Flux.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/events/")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").isEmpty();
    }




    @Test
    void getAllEventsByUserId_ReturnsEvents() {
        Long event1Id = 1L;
        Long userId = 2L;
        Long file1Id = 3L;
        String file1Name = "testFile1.txt";
        String someFile1Location = "/prefix/" + file1Name;
        File file1 = File.builder()
                .id(file1Id)
                .location(someFile1Location)
                .build();
        EventDTO eventDTO1 = EventDTO.builder()
                .id(event1Id)
                .userId(userId)
                .fileId(file1Id)
                .file(file1)
                .build();

        Long event2Id = 4L;
        Long file2Id = 5L;
        String file2Name = "testFile2.txt";
        String someFile2Location = "/prefix/" + file2Name;
        File file2 = File.builder()
                .id(file2Id)
                .location(someFile2Location)
                .build();
        EventDTO eventDTO2 = EventDTO.builder()
                .id(event2Id)
                .userId(userId)
                .fileId(file2Id)
                .file(file2)
                .build();

        List<EventDTO> events = List.of(eventDTO1, eventDTO2);

        when(eventService.getAllEventsByAuth(any()))
                .thenReturn(Flux.fromIterable(events));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/events/?userId=" + userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").value(hasSize(2))
                .jsonPath("$.[0].id").isEqualTo(event1Id)
                .jsonPath("$.[0].user_id").isEqualTo(userId)
                .jsonPath("$.[0].file_id").isEqualTo(file1Id)
                .jsonPath("$.[0].links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/" + file1Id)
                .jsonPath("$.[0].links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/" + file1Name)
                .jsonPath("$.[1].id").isEqualTo(event2Id)
                .jsonPath("$.[1].user_id").isEqualTo(userId)
                .jsonPath("$.[1].file_id").isEqualTo(file2Id)
                .jsonPath("$.[1].links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/" + file2Id)
                .jsonPath("$.[1].links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/" + file2Name);
    }

    @Test
    void getAllEventsByUserId_ReturnsEmptyArray() {
        String userId = "1";

        when(eventService.getAllEventsByAuth(any()))
                .thenReturn(Flux.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/events/?userId=" + userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").isEmpty();
    }




    @Test
    void updateEvent_UpdatesEventSuccessfully() {
        Long eventId = 1L;
        Long userId = 2L;
        Long fileId = 3L;

        EventUpdateRequestDTO eventUpdateRequestDTO = EventUpdateRequestDTO.builder()
                .userId(userId)
                .fileId(fileId)
                .build();

        EventBasicDTO eventBasicDTO = EventBasicDTO.builder()
                .id(eventId)
                .userId(userId)
                .fileId(fileId)
                .build();

        when(eventService.updateEventById(eq(eventId), any(EventUpdateRequestDTO.class)))
                .thenReturn(Mono.just(eventBasicDTO));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .put().uri("/api/v1/events/" + eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(eventUpdateRequestDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(eventId)
                .jsonPath("$.user_id").isEqualTo(userId)
                .jsonPath("$.file_id").isEqualTo(fileId);
    }

    @Test
    void updateEvent_EventNotFound() {
        Long eventId = 1L;
        Long userId = 2L;
        Long fileId = 3L;

        EventUpdateRequestDTO eventUpdateRequestDTO = EventUpdateRequestDTO.builder()
                .userId(userId)
                .fileId(fileId)
                .build();

        when(eventService.updateEventById(eq(eventId), any(EventUpdateRequestDTO.class)))
                .thenReturn(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(ERR_EVENT_WITH_ID_NOT_FOUND, eventId))));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .put().uri("/api/v1/events/" + eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(eventUpdateRequestDTO)
                .exchange()
                .expectStatus().isNotFound();
    }





    @Test
    void deleteEventById_DeletesEventSuccessfully() {
        Long eventId = 1L;

        when(eventService.deleteEventById(eq(eventId))).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/events/" + eventId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deleteEventById_EventNotFound_HandlesResponseStatusException() {
        Long eventId = 1L;
        String uri = "/api/v1/events/" + eventId ;
        String errorMessage = String.format(ERR_EVENT_WITH_ID_NOT_FOUND, eventId);

        when(eventService.deleteEventById(eq(eventId)))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/events/" + eventId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo(uri)
                .jsonPath("$.requestId").isNotEmpty()
                .jsonPath("$.error").isEqualTo("NOT_FOUND")
                .jsonPath("$.message").isEqualTo(errorMessage)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void deleteAllEventsByUserId_DeletesEventsSuccessfully() {
        Long userId = 1L;

        when(eventService.deleteAllEventsByUserId(eq(userId))).thenReturn(Mono.just(3));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/events/?userId=" + userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("3");
    }

    @Test
    void deleteAllEvents_DeletesAllEventsSuccessfully() {
        when(eventService.deleteAllEvents()).thenReturn(Mono.just(4));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/events/all")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("4");
    }
}
