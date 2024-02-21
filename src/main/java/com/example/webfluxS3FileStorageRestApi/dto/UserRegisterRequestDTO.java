package com.example.webfluxS3FileStorageRestApi.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserRegisterRequestDTO {

    private String username;
    private String password;
    private String firstName;
    private String lastName;

    @ToString.Include(name = "password")
    private String maskPassword() {
        return "********";
    }
}
