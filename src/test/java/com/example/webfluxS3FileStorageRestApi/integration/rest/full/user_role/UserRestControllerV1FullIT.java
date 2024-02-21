package com.example.webfluxS3FileStorageRestApi.integration.rest.full.user_role;

import com.example.webfluxS3FileStorageRestApi.dto.AuthRequestDTO;
import com.example.webfluxS3FileStorageRestApi.dto.AuthResponseDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserUpdateRequestDTO;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@ActiveProfiles("dynamic-db-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class UserRestControllerV1FullIT {

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
    void getUserById_ShouldGetOwnUser() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/users/2")
                .then()
                .statusCode(200)
                .body("id", equalTo(2))
                .body("username", equalTo("TestUserRoleUser"));
    }

    @Test
    void getUserById_ShouldRestrict_NotOwnUser() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/users/1")
                .then()
                .statusCode(403);
    }

    @Test
    void getAllUsers_ShouldRestrict() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/users/")
                .then()
                .statusCode(403);
    }

    @Test
    void updateUser_ShouldRestrict() {
        long userId = 2L;

        UserUpdateRequestDTO userUpdateRequestDTO = UserUpdateRequestDTO.builder()
                .build();

        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .body(userUpdateRequestDTO)
                .when()
                .put("/api/v1/users/" + userId)
                .then()
                .statusCode(403);
    }

    @Test
    void deleteUserById_ShouldRestrict() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .delete("/api/v1/users/2")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteAllUsers_ShouldRestrict() {
        given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .delete("/api/v1/users/all")
                .then()
                .statusCode(403);
    }
}
