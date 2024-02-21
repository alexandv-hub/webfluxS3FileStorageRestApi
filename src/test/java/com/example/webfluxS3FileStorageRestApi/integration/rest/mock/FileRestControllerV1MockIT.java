package com.example.webfluxS3FileStorageRestApi.integration.rest.mock;

import com.example.webfluxS3FileStorageRestApi.integration.config.ErrorHandlerConfig;
import com.example.webfluxS3FileStorageRestApi.integration.config.TestWebSecurityConfig;
import com.example.webfluxS3FileStorageRestApi.mapper.UserMapper;
import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.rest.FileRestControllerV1;
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
import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.Files.ERR_FILE_WITH_ID_NOT_FOUND;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


@ActiveProfiles("test")
@WebFluxTest(controllers = FileRestControllerV1.class)
@Import({ErrorHandlerConfig.class, TestWebSecurityConfig.class})
public class FileRestControllerV1MockIT {

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
    public void getFileById_WhenFileExists_ReturnsFileWithLinks() {
        Long fileId = 1L;
        String fileName = "testFile.txt";
        String location = "https://someTestS3BucketName.s3.amazonaws.com/" + fileName;
        File file = File.builder()
                .id(fileId)
                .location(location).build();

        when(fileService.getFileByIdAndAuth(eq(fileId), any())).thenReturn(Mono.just(file));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/files/" + fileId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(fileId)
                .jsonPath("$.location").isEqualTo(location)
                .jsonPath("$.links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/" + fileId)
                .jsonPath("$.links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/" + fileName);
    }

    @Test
    void getFileById_WhenFileDoesNotExist_ReturnsNotFound_HandlesResponseStatusException() {
        Long fileId = 1L;
        String uri = "/api/v1/files/" + fileId ;
        String errorMessage = String.format(ERR_FILE_WITH_ID_NOT_FOUND, fileId);

        when(fileService.getFileByIdAndAuth(anyLong(), any()))
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
    void getFileById_WhenUserForbidden_ReturnsForbidden_HandlesResponseStatusException() {
        Long fileId = 3L;
        String uri = "/api/v1/files/" + fileId ;
        String errorMessage = ERR_ACCESS_DENIED;

        when(fileService.getFileByIdAndAuth(eq(fileId), any()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, errorMessage)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/files/" + fileId)
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
    void getAllFiles_ReturnsFiles() {
        Long file1Id = 1L;
        Long file2Id = 2L;
        String location1 = "/path/location/testFile1.txt";
        String location2 = "/path/location/testFile2.txt";

        File file1 = File.builder()
                .id(file1Id)
                .location(location1)
                .build();
        File file2 = File.builder()
                .id(file2Id)
                .location(location2)
                .build();
        List<File> files = List.of(file1, file2);

        when(fileService.getAllFilesByAuth(any())).thenReturn(Flux.fromIterable(files));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/files/")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").value(hasSize(2))
                .jsonPath("$.[0].id").isEqualTo(file1.getId())
                .jsonPath("$.[0].location").isEqualTo(file1.getLocation())
                .jsonPath("$.[0].links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/1")
                .jsonPath("$.[0].links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/testFile1.txt")
                .jsonPath("$.[1].id").isEqualTo(file2.getId())
                .jsonPath("$.[1].location").isEqualTo(file2.getLocation())
                .jsonPath("$.[1].links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/2")
                .jsonPath("$.[1].links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/testFile2.txt");
    }

    @Test
    void getAllFiles_ReturnsEmptyArray() {
        when(fileService.getAllFilesByAuth(any())).thenReturn(Flux.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/files/")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").isEmpty();
    }


    
    
    @Test
    void getAllFilesByUserId_ReturnsFiles() {
        long userId = 1L;

        Long file1Id = 2L;
        String file1Name = "testFile1.txt";
        String someFile1Location = "/prefix/" + file1Name;
        File file1 = File.builder()
                .id(file1Id)
                .location(someFile1Location)
                .build();

        Long file2Id = 3L;
        String file2Name = "testFile2.txt";
        String someFile2Location = "/prefix/" + file2Name;
        File file2 = File.builder()
                .id(file2Id)
                .location(someFile2Location)
                .build();

        List<File> files = List.of(file1, file2);

        when(fileService.getAllFilesByAuth(any()))
                .thenReturn(Flux.fromIterable(files));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/files/?userId=" + userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").value(hasSize(2))
                .jsonPath("$.[0].id").isEqualTo(file1Id)
                .jsonPath("$.[0].location").isEqualTo(someFile1Location)
                .jsonPath("$.[0].links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/" + file1Id)
                .jsonPath("$.[0].links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/" + file1Name)
                .jsonPath("$.[1].id").isEqualTo(file2Id)
                .jsonPath("$.[1].location").isEqualTo(someFile2Location)
                .jsonPath("$.[1].links[?(@.rel=='self')].href").isEqualTo("/api/v1/files/" + file2Id)
                .jsonPath("$.[1].links[?(@.rel=='download')].href").isEqualTo("/api/v1/file-storage/download-flux/" + file2Name);
    }

    @Test
    void getAllFilesByUserId_ReturnsEmptyArray() {
        String userId = "1";

        when(fileService.getAllFilesByAuth(any()))
                .thenReturn(Flux.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/files/?userId=" + userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").isEmpty();
    }
    
    

    @Test
    void updateFile_UpdatesFileSuccessfully() {
        Long fileId = 1L;
        File file = File.builder()
                .location("Updated Location")
                .build();

        File updatedFile = File.builder()
                .id(fileId)
                .location("Updated Location")
                .build();

        when(fileService.updateFileById(eq(fileId), any(File.class))).thenReturn(Mono.just(updatedFile));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .put().uri("/api/v1/files/" + fileId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(file)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(updatedFile.getId())
                .jsonPath("$.location").isEqualTo(updatedFile.getLocation());
    }

    @Test
    void updateFile_FileNotFound() {
        Long fileId = 1L;

        File file = File.builder()
                .location("Updated Location")
                .build();

        when(fileService.updateFileById(eq(fileId), any(File.class)))
                .thenReturn(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(ERR_FILE_WITH_ID_NOT_FOUND, fileId))));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .put().uri("/api/v1/files/" + fileId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(file)
                .exchange()
                .expectStatus().isNotFound();
    }



    @Test
    void deleteFileById_DeletesFileSuccessfully() {
        Long fileId = 1L;

        when(fileService.deleteFileById(eq(fileId))).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/files/" + fileId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deleteFileById_FileNotFound() {
        Long fileId = 1L;

        when(fileService.deleteFileById(eq(fileId)))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/files/" + fileId)
                .exchange()
                .expectStatus().isNotFound();
    }



    @Test
    void deleteAllFilesByUserId_DeletesFilesSuccessfully() {
        Long userId = 1L;
        int deletedCount = 5;

        when(fileService.deleteAllFilesByUserId(eq(userId))).thenReturn(Mono.just(deletedCount));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri(uriBuilder -> uriBuilder.path("/api/v1/files/")
                        .queryParam("userId", userId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .isEqualTo(deletedCount);
    }

    @Test
    void deleteAllFilesByUserId_NoFilesFound() {
        Long userId = 2L;
        int deletedCount = 0;

        when(fileService.deleteAllFilesByUserId(eq(userId))).thenReturn(Mono.just(deletedCount));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri(uriBuilder -> uriBuilder.path("/api/v1/files/")
                        .queryParam("userId", userId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .isEqualTo(deletedCount);
    }



    @Test
    void deleteAllFiles_DeletesAllFilesSuccessfully() {
        int deletedCount = 10;

        when(fileService.deleteAllFiles()).thenReturn(Mono.just(deletedCount));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/files/all")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .isEqualTo(deletedCount);
    }

    @Test
    void deleteAllFiles_NoFilesToDelete() {
        int deletedCount = 0;

        when(fileService.deleteAllFiles()).thenReturn(Mono.just(deletedCount));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .delete().uri("/api/v1/files/all")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .isEqualTo(deletedCount);
    }
}
