package com.example.webfluxS3FileStorageRestApi.integration.rest.mock;

import com.example.webfluxS3FileStorageRestApi.dto.EventDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserBasicDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserUpdateRequestDTO;
import com.example.webfluxS3FileStorageRestApi.integration.config.ErrorHandlerConfig;
import com.example.webfluxS3FileStorageRestApi.integration.config.TestWebSecurityConfig;
import com.example.webfluxS3FileStorageRestApi.mapper.EventMapper;
import com.example.webfluxS3FileStorageRestApi.mapper.UserMapper;
import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.model.UserRole;
import com.example.webfluxS3FileStorageRestApi.rest.UserRestControllerV1;
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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.ERR_ACCESS_DENIED;
import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.Users.ERR_USER_WITH_ID_NOT_FOUND;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@WebFluxTest(controllers = UserRestControllerV1.class)
@Import({ErrorHandlerConfig.class, TestWebSecurityConfig.class})
public class UserRestControllerV1MockIT {

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
        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "TestUser");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authentication = new UsernamePasswordAuthenticationToken(
                customPrincipal, null, authorities);
    }


    @Test
    public void getUserById_WhenUserExists_ReturnsUserWithEvents() {
        Long userId = 1L;
        Long fileId = 2L;
        Long eventId = 3L;
        String fileName = "testFile.txt";
        String userName = "TestUser";
        String fileLocation = "https://someTestS3BucketName.s3.amazonaws.com/" + fileName;

        File file = File.builder()
                .id(fileId)
                .location(fileLocation)
                .build();

        EventDTO eventDTO = EventDTO.builder()
                .id(eventId)
                .userId(userId)
                .fileId(fileId)
                .file(file)
                .build();
        List<EventDTO> eventDTOs = List.of(eventDTO);

        UserDTO userDTO = UserDTO.builder()
                .id(userId)
                .username(userName)
                .role(UserRole.ADMIN)
                .firstName("Alex")
                .lastName("Petrov")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .eventDTOs(eventDTOs)
                .build();

        when(userService.getUserByIdAndAuth(eq(userId), any())).thenReturn(Mono.just(userDTO));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/users/" + userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId)
                .jsonPath("$.username").isEqualTo(userName)
                .jsonPath("$.role").isEqualTo("ADMIN")
                .jsonPath("$.first_name").isEqualTo("Alex")
                .jsonPath("$.last_name").isEqualTo("Petrov")
                .jsonPath("$.enabled").isEqualTo(true)
                .jsonPath("$.created_at").isNotEmpty()
                .jsonPath("$.updated_at").isEqualTo(null)
                .jsonPath("$.events.[0].id").isEqualTo(eventId)
                .jsonPath("$.events.[0].user_id").isEqualTo(userId)
                .jsonPath("$.events.[0].file_id").isEqualTo(fileId)
                .jsonPath("$.events.[0].file.id").isEqualTo(fileId)
                .jsonPath("$.events.[0].file.location").isEqualTo(fileLocation);
    }

    @Test
    void getUserById_WhenUserDoesNotExist_ReturnsNotFound_HandlesResponseStatusException() {
        Long userId = 1L;
        String uri = "/api/v1/users/" + userId ;
        String errorMessage = String.format(ERR_USER_WITH_ID_NOT_FOUND, userId);

        when(userService.getUserByIdAndAuth(anyLong(), any()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri(uri)
                .accept(MediaType.APPLICATION_JSON)
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
    void getUserById_WhenUserForbidden_ReturnsForbidden_HandlesResponseStatusException() {
        Long userId = 3L;
        String uri = "/api/v1/users/" + userId ;
        String errorMessage = ERR_ACCESS_DENIED;

        when(userService.getUserByIdAndAuth(eq(userId), any()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, errorMessage)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/users/" + userId)
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
    void getAllUsers_ReturnsUsers() {
        Long user1Id = 1L;
        Long user2Id = 2L;
        String user1Username = "TestUser1";
        String user2Username = "TestUser2";

        UserBasicDTO user1 = UserBasicDTO.builder()
                .username(user1Username)
                .role(UserRole.ADMIN)
                .firstName("Alex")
                .lastName("Petrov")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();
        user1.setId(user1Id);

        UserBasicDTO user2 = UserBasicDTO.builder()
                .username(user2Username)
                .role(UserRole.USER)
                .firstName("Michael")
                .lastName("Jackson")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();
        user2.setId(user2Id);

        List<UserBasicDTO> users = List.of(user1, user2);

        when(userService.getAllUsers()).thenReturn(Flux.fromIterable(users));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/users/")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").value(hasSize(2))
                .jsonPath("$.[0].id").isEqualTo(user1Id)
                .jsonPath("$.[0].username").isEqualTo(user1Username)
                .jsonPath("$.[0].role").isEqualTo("ADMIN")
                .jsonPath("$.[0].first_name").isEqualTo("Alex")
                .jsonPath("$.[0].last_name").isEqualTo("Petrov")
                .jsonPath("$.[0].enabled").isEqualTo(true)
                .jsonPath("$.[0].created_at").isNotEmpty()
                .jsonPath("$.[0].updated_at").isEqualTo(null)
                .jsonPath("$.[1].id").isEqualTo(user2Id)
                .jsonPath("$.[1].username").isEqualTo(user2Username)
                .jsonPath("$.[1].role").isEqualTo("USER")
                .jsonPath("$.[1].first_name").isEqualTo("Michael")
                .jsonPath("$.[1].last_name").isEqualTo("Jackson")
                .jsonPath("$.[1].enabled").isEqualTo(true)
                .jsonPath("$.[1].created_at").isNotEmpty()
                .jsonPath("$.[1].updated_at").isEqualTo(null);
    }

    @Test
    void getAllUsers_ReturnsEmptyArray() {
        when(userService.getAllUsers()).thenReturn(Flux.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/users/")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").isEmpty();
    }




    @Test
    void updateUser_UpdatesUserSuccessfully() {
        Long userId = 1L;
        LocalDateTime createdAt = LocalDateTime.now();
        UserUpdateRequestDTO userToUpdate = UserUpdateRequestDTO.builder()
                .username("TestUser")
                .role(UserRole.ADMIN)
                .firstName("Alex")
                .lastName("Petrov")
                .enabled(false)
                .createdAt(createdAt)
                .build();

        UserBasicDTO updatedUser = UserBasicDTO.builder()
                .username("TestUser")
                .role(UserRole.ADMIN)
                .firstName("Alex")
                .lastName("Petrov")
                .enabled(false)
                .createdAt(createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
        updatedUser.setId(userId);

        when(userService.updateUserById(eq(userId), any(UserUpdateRequestDTO.class))).thenReturn(Mono.just(updatedUser));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .put().uri("/api/v1/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userToUpdate)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId)
                .jsonPath("$.username").isEqualTo("TestUser")
                .jsonPath("$.role").isEqualTo("ADMIN")
                .jsonPath("$.first_name").isEqualTo("Alex")
                .jsonPath("$.last_name").isEqualTo("Petrov")
                .jsonPath("$.enabled").isEqualTo(false)
                .jsonPath("$.created_at").isNotEmpty()
                .jsonPath("$.updated_at").isNotEmpty();
    }

    @Test
    void updateUser_UserNotFound_HandlesResponseStatusException() {
        Long userId = 1L;
        String uri = "/api/v1/users/" + userId ;
        String errorMessage = String.format(ERR_USER_WITH_ID_NOT_FOUND, userId);
        UserUpdateRequestDTO userToUpdate = UserUpdateRequestDTO.builder()
                .username("TestUser")
                .role(UserRole.ADMIN)
                .firstName("Alex")
                .lastName("Petrov")
                .enabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.updateUserById(eq(userId), any(UserUpdateRequestDTO.class)))
                .thenReturn(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(ERR_USER_WITH_ID_NOT_FOUND, userId))));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .put().uri("/api/v1/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userToUpdate)
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
    void deleteUserById_DeletesUserSuccessfully() {
        Long userId = 1L;

        when(userService.deleteUserById(eq(userId))).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/users/" + userId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deleteUserById_UserNotFound() {
        Long userId = 1L;

        when(userService.deleteUserById(eq(userId)))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/users/" + userId)
                .exchange()
                .expectStatus().isNotFound();
    }



    @Test
    void deleteAllUsers_DeletesUsersSuccessfully() {
        int deletedCount = 5;

        when(userService.deleteAllUsers()).thenReturn(Mono.just(deletedCount));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri(uriBuilder -> uriBuilder.path("/api/v1/users/all")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .isEqualTo(deletedCount);
    }

    @Test
    void deleteAllUsers_NoUsersFound() {
        int deletedCount = 0;

        when(userService.deleteAllUsers()).thenReturn(Mono.just(deletedCount));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri(uriBuilder -> uriBuilder.path("/api/v1/users/all")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .isEqualTo(deletedCount);
    }
}
