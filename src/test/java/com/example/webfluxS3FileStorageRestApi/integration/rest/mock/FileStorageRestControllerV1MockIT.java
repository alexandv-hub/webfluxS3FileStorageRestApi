package com.example.webfluxS3FileStorageRestApi.integration.rest.mock;

import com.example.webfluxS3FileStorageRestApi.dto.UploadedFileResponseDTO;
import com.example.webfluxS3FileStorageRestApi.integration.config.ErrorHandlerConfig;
import com.example.webfluxS3FileStorageRestApi.integration.config.TestWebSecurityConfig;
import com.example.webfluxS3FileStorageRestApi.mapper.UserMapper;
import com.example.webfluxS3FileStorageRestApi.rest.FileStorageRestControllerV1;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@WebFluxTest(controllers = FileStorageRestControllerV1.class)
@Import({ErrorHandlerConfig.class, TestWebSecurityConfig.class})
public class FileStorageRestControllerV1MockIT {

    @MockBean
    private SecurityService securityService;
    @MockBean
    private UserService userService;
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
    public void uploadFile_WhenSuccessfully_ReturnsUploadedFileResponseDTO() throws IOException {
        String fileName = "testFile.txt";
        Path tempFile = Files.createTempFile("test-upload", ".txt");
        Files.writeString(tempFile, "Some content");
        Resource resource = new FileSystemResource(tempFile.toFile());

        UploadedFileResponseDTO uploadedFileResponseDTO = UploadedFileResponseDTO.builder()
                .fileName(fileName)
                .uploadDateTime(LocalDateTime.now())
                .build();

        when(fileStorageService.uploadUserFileToStorage(any(), any())).thenReturn(Mono.just(uploadedFileResponseDTO));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .post().uri("/api/v1/file-storage/upload-flux")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", resource))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.file_name").isEqualTo(fileName)
                .jsonPath("$.upload_date_time").isNotEmpty();

        Files.deleteIfExists(tempFile);
    }

    @Test
    public void downloadFileByName_WhenSuccessfully_ReturnsFile() {
        String fileName = "testFile.txt";
        Resource mockFileResource = new ByteArrayResource("File content".getBytes(StandardCharsets.UTF_8));

        when(fileStorageService.downloadFileFromStorageByFileNameAndAuth(eq(fileName), any()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(mockFileResource)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/file-storage/download-flux/{fileName}", fileName)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("File content");
    }
}
