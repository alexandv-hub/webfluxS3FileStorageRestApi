package com.example.webfluxS3FileStorageRestApi.integration.repository.impl;

import com.example.webfluxS3FileStorageRestApi.dto.UploadedFileResponseDTO;
import com.example.webfluxS3FileStorageRestApi.repository.impl.FileStorageRepositoryS3Impl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Slf4j
@ActiveProfiles("dynamic-db-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class FileStorageRepositoryS3ImplIT {

    static final String TEST_FILE_NAME = "testFile.txt";
    static final String TEST_FILE_CONTENT = "Test file content";
    static final String BUCKET_NAME = UUID.randomUUID().toString();

    private static S3AsyncClient s3Client;
    private FileStorageRepositoryS3Impl repository;

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0"))
                        .withServices(S3);

    @Container
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>(
            "mysql:latest")
            .withUsername("root")
            .withPassword("password")
            .withReuse(Boolean.FALSE);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:mysql://%s:%d/%s",
                mySQLContainer.getHost(), mySQLContainer.getFirstMappedPort(), mySQLContainer.getDatabaseName()));

        registry.add("spring.r2dbc.username", mySQLContainer::getUsername);
        registry.add("spring.r2dbc.password", mySQLContainer::getPassword);
    }

    @BeforeAll
    static void setup() {
        mySQLContainer.start();
        localStack.start();

        s3Client = S3AsyncClient.builder()
                .endpointOverride(URI.create(localStack.getEndpointOverride(S3).toString()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build();
    }

    @AfterAll
    static void afterAll() {
        localStack.stop();
        mySQLContainer.stop();
    }

    @BeforeEach
    void init() {
        repository = new FileStorageRepositoryS3Impl();
        ReflectionTestUtils.setField(repository, "s3Client", s3Client);
        ReflectionTestUtils.setField(repository, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(repository, "keyPrefix", "test-prefix");

        s3Client.createBucket(CreateBucketRequest.builder()
                        .bucket(BUCKET_NAME)
                        .build())
                .join();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key("test-prefix/" + TEST_FILE_NAME)
                        .build(),
                AsyncRequestBody.fromString(TEST_FILE_CONTENT))
                .join();
    }


    @Test
    void testUploadFileToStorage() {
        FilePart filePart = mock(FilePart.class);
        when(filePart.filename()).thenReturn(TEST_FILE_NAME);
        Path tempFilePath = Paths.get("/tmp/testfile.txt");
        try {
            Files.writeString(tempFilePath, "This is a test file");
            when(filePart.transferTo(any(Path.class))).then(invocation -> {
                Path path = invocation.getArgument(0);
                Files.copy(tempFilePath, path, StandardCopyOption.REPLACE_EXISTING);
                return Mono.empty();
            });
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

        Mono<UploadedFileResponseDTO> resultMono = repository.uploadUserFileToStorage(filePart);
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(TEST_FILE_NAME, response.getFileName());
                    // Проверки на дату и время загрузки можно добавить здесь
                })
                .verifyComplete();
    }

    @Test
    void testDownloadFileFromStorage() {
        Mono<ResponseEntity<Resource>> result = repository.downloadFileFromStorage(TEST_FILE_NAME);

        StepVerifier.create(result)
                .expectNextMatches(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertTrue(responseEntity.hasBody());
                    Resource resource = responseEntity.getBody();
                    assertNotNull(resource);
                    try {
                        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                        assertEquals(TEST_FILE_CONTENT, content);
                    } catch (IOException e) {
                        fail("Failed to read resource content", e);
                    }
                    return true;
                })
                .verifyComplete();
    }
}
