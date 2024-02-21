package com.example.webfluxS3FileStorageRestApi.integration.rest.full.user_role;

import com.example.webfluxS3FileStorageRestApi.dto.AuthRequestDTO;
import com.example.webfluxS3FileStorageRestApi.dto.AuthResponseDTO;
import com.example.webfluxS3FileStorageRestApi.model.File;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@ActiveProfiles("dynamic-db-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class FileRestControllerV1FullIT {

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
                        "TestUserRoleUser",
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
    void getFileById_ShouldGet_OwnFile() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/files/3")
                .then()
                .statusCode(200)
                .body("id", equalTo(3))
                .body("location", equalTo("testFile3.txt"));
    }

    @Test
    void getFileById_ShouldRestrict_OtherUserFile() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/files/1")
                .then()
                .statusCode(403);
    }

    @Test
    void getAllFiles_ShouldGet2Files_OnlyOwnFiles() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/files/")
                .then()
                .statusCode(200)
                .body(".", hasSize(2));
    }

    @Test
    void getAllFilesByUserId_ShouldRestrict() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/files/by-user-id/?userId=1")
                .then()
                .statusCode(403);
    }

    @Test
    void updateFile_ShouldRestrict() {
        File file = File.builder()
                .location("testFile1Updated.txt")
                .build();

        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .body(file)
                .when()
                .put("/api/v1/files/1")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteFileById_ShouldRestrict() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .delete("/api/v1/files/1")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteAllFilesByUserId_ShouldRestrict() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .delete("/api/v1/files/?userId=2")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteAllFiles_ShouldRestrict() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .delete("/api/v1/files/all")
                .then()
                .statusCode(403);
    }
}
