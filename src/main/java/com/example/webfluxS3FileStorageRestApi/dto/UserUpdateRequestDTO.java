package com.example.webfluxS3FileStorageRestApi.dto;

import com.example.webfluxS3FileStorageRestApi.model.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserUpdateRequestDTO {

    private String username;
    private String password;
    private UserRole role;
    private String firstName;
    private String lastName;
    private boolean enabled;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @ToString.Include(name = "password")
    private String maskPassword() {
        return "********";
    }
}
