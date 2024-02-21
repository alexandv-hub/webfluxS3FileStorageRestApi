package com.example.webfluxS3FileStorageRestApi.integration.rest.full.admin_role;

import com.example.webfluxS3FileStorageRestApi.dto.AuthRequestDTO;
import com.example.webfluxS3FileStorageRestApi.dto.AuthResponseDTO;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@ActiveProfiles("dynamic-db-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class FileStorageRestControllerV1FullIT {

    @LocalServerPort
    private int port;

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
    static void beforeAll() {
        mySQLContainer.start();
    }

    @AfterAll
    static void afterAll() {
        mySQLContainer.stop();
    }

    @Autowired
    private WebTestClient webTestClient;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;

        jwtToken = webTestClient.post().uri("/api/v1/auth/login")
                .bodyValue(new AuthRequestDTO(
                        "TestUserRoleAdmin",
                        "password"
                ))
                .exchange()
                .expectStatus().isOk()
                .returnResult(AuthResponseDTO.class)
                .getResponseBody()
                .map(AuthResponseDTO::getToken)
                .next().block();
    }


    @Test
    @Order(1)
    void shouldUploadFileToS3Successfully() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType("multipart/form-data")
                .multiPart("file", new File("src/test/resources/testFile.txt"))
                .when()
                .post("/api/v1/file-storage/upload-flux")
                .then()
                .statusCode(200)
                .body("file_name", equalTo("testFile.txt"))
                .body("upload_date_time", notNullValue());

        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/events/5")
                .then()
                .statusCode(200)
                .body("id", equalTo(5))
                .body("user_id", equalTo(1))
                .body("file_id", equalTo(5));

        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/files/5")
                .then()
                .statusCode(200)
                .body("id", equalTo(5))
                .body("location", endsWith("testFile.txt"));
    }

    @Test
    @Order(2)
    void shouldDownloadFileFromS3Successfully() {
        String fileName = "testFile.txt";
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .when()
                .get("/api/v1/file-storage/download-flux/" + fileName)
                .then()
                .statusCode(200)
                .header("Content-Type", notNullValue())
                .body(notNullValue());
    }
}
