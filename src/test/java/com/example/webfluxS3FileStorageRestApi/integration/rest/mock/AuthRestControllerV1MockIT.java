package com.example.webfluxS3FileStorageRestApi.integration.rest.mock;

import com.example.webfluxS3FileStorageRestApi.dto.AuthResponseDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserBasicDTO;
import com.example.webfluxS3FileStorageRestApi.exception.security.AuthException;
import com.example.webfluxS3FileStorageRestApi.integration.config.ErrorHandlerConfig;
import com.example.webfluxS3FileStorageRestApi.integration.config.TestWebSecurityConfig;
import com.example.webfluxS3FileStorageRestApi.mapper.EventMapper;
import com.example.webfluxS3FileStorageRestApi.mapper.UserMapper;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import com.example.webfluxS3FileStorageRestApi.model.UserRole;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.security.SecurityService;
import com.example.webfluxS3FileStorageRestApi.security.TokenDetails;
import com.example.webfluxS3FileStorageRestApi.service.EventService;
import com.example.webfluxS3FileStorageRestApi.service.FileService;
import com.example.webfluxS3FileStorageRestApi.service.FileStorageService;
import com.example.webfluxS3FileStorageRestApi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@WebFluxTest
@Import({ErrorHandlerConfig.class, TestWebSecurityConfig.class})
public class AuthRestControllerV1MockIT {

    @MockBean
    private SecurityService securityService;
    @MockBean
    private UserService userService;
    @MockBean
    private UserMapper userMapper;
    @MockBean
    private EventMapper eventMapper;
    @MockBean
    private EventService eventService;
    @MockBean
    private FileService fileService;
    @MockBean
    private FileStorageService fileStorageService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void registerTest() {
        String userRegisterRequestDTOJson = """
                {
                    "username": "testUser",
                    "password": "password",
                    "first_name": "Alex",
                    "last_name": "Petrov"
                }
                """;

        Long userId = 1L;
        LocalDateTime localDateTimeNow = LocalDateTime.now();
        UserBasicDTO expectedResponse = UserBasicDTO.builder()
                .id(userId)
                .username("testUser")
                .role(UserRole.USER)
                .firstName("Alex")
                .lastName("Petrov")
                .enabled(true)
                .createdAt(localDateTimeNow)
                .updatedAt(null)
                .build();

        UserEntity userEntity = UserEntity.builder()
                .username("testUser")
                .role(UserRole.USER)
                .firstName("Alex")
                .lastName("Petrov")
                .enabled(true)
                .createdAt(localDateTimeNow)
                .updatedAt(null)
                .build();
        userEntity.setId(userId);

        when(userService.registerUser(any())).thenReturn(Mono.just(userEntity));
        when(userMapper.mapToUserBasicDTO(any())).thenReturn(expectedResponse);

        webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRegisterRequestDTOJson)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("1")
                .jsonPath("$.username").isEqualTo("testUser")
                .jsonPath("$.role").isEqualTo("USER")
                .jsonPath("$.first_name").isEqualTo("Alex")
                .jsonPath("$.last_name").isEqualTo("Petrov")
                .jsonPath("$.enabled").isEqualTo(true)
                .jsonPath("$.created_at").exists()
                .jsonPath("$.updated_at").doesNotExist();
    }



    @Test
    void loginTest_ValidCredentials_Successful() {
        String authRequestDTOJson = """
                {
                    "username": "testUser",
                    "password": "password"
                }
                """;

        TokenDetails tokenDetails = TokenDetails.builder()
                .userId(1L)
                .token("someToken")
                .issuedAt(new Date())
                .expiresAt(new Date())
                .build();

        AuthResponseDTO expectedResponse = AuthResponseDTO.builder()
                .userId(tokenDetails.getUserId())
                .token(tokenDetails.getToken())
                .issuedAt(tokenDetails.getIssuedAt())
                .expiresAt(tokenDetails.getExpiresAt())
                .build();

        when(securityService.authenticate(anyString(), anyString())).thenReturn(Mono.just(tokenDetails));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequestDTOJson)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user_id").isEqualTo(expectedResponse.getUserId())
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.issued_at").exists()
                .jsonPath("$.expires_at").exists();
    }

    @Test
    void loginTest_AccountDisabled_Fails() {
        String authRequestDTOJson = """
                {
                    "username": "TestUser",
                    "password": "password"
                }
                """;

        when(securityService.authenticate(anyString(), anyString()))
                .thenReturn(Mono.error(new AuthException("Account disabled", "PROSELYTE_USER_ACCOUNT_DISABLED")));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequestDTOJson)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.errors[0].code").isEqualTo("PROSELYTE_USER_ACCOUNT_DISABLED")
                .jsonPath("$.errors[0].message").isEqualTo("Account disabled");
    }

    @Test
    void loginTest_InvalidPassword_Fails() {
        String authRequestDTOJson = """
                {
                    "username": "testUser",
                    "password": "invalidPassword"
                }
                """;

        when(securityService.authenticate(anyString(), anyString()))
                .thenReturn(Mono.error(new AuthException("Invalid password", "PROSELYTE_INVALID_PASSWORD")));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequestDTOJson)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.errors[0].code").isEqualTo("PROSELYTE_INVALID_PASSWORD")
                .jsonPath("$.errors[0].message").isEqualTo("Invalid password");
    }

    @Test
    void loginTest_InvalidUsername_Fails() {
        String authRequestDTOJson = """
                {
                    "username": "invalidTestUsername",
                    "password": "password"
                }
                """;

        when(securityService.authenticate(anyString(), anyString()))
                .thenReturn(Mono.error(new AuthException("Invalid username", "PROSELYTE_INVALID_USERNAME")));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequestDTOJson)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.errors[0].code").isEqualTo("PROSELYTE_INVALID_USERNAME")
                .jsonPath("$.errors[0].message").isEqualTo("Invalid username");
    }



    @Test
    void getUserInfoTest_ValidToken_Success() {
        Long userId = 1L;
        LocalDateTime localDateTimeNow = LocalDateTime.now();
        UserEntity userEntity = UserEntity.builder()
                .username("testUser")
                .role(UserRole.USER)
                .firstName("Alex")
                .lastName("Petrov")
                .enabled(true)
                .createdAt(localDateTimeNow)
                .updatedAt(null)
                .build();
        userEntity.setId(userId);

        UserBasicDTO expectedUserBasicDTO = UserBasicDTO.builder()
                .id(userId)
                .username("testUser")
                .role(UserRole.USER)
                .firstName("Alex")
                .lastName("Petrov")
                .enabled(true)
                .createdAt(localDateTimeNow)
                .updatedAt(null)
                .build();

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "testUser");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customPrincipal, null, authorities);

        when(userService.getUserById(userId)).thenReturn(Mono.just(userEntity));
        when(userMapper.mapToUserBasicDTO(any(UserEntity.class))).thenReturn(expectedUserBasicDTO);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get()
                .uri("/api/v1/auth/info")
                .headers(headers -> headers.setBearerAuth("test_token"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("1")
                .jsonPath("$.username").isEqualTo("testUser")
                .jsonPath("$.role").isEqualTo("USER")
                .jsonPath("$.first_name").isEqualTo("Alex")
                .jsonPath("$.last_name").isEqualTo("Petrov")
                .jsonPath("$.enabled").isEqualTo(true)
                .jsonPath("$.created_at").exists()
                .jsonPath("$.updated_at").doesNotExist();
    }
}
