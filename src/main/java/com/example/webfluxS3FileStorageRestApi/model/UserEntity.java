package com.example.webfluxS3FileStorageRestApi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table("users")
public class UserEntity extends BaseEntity {

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private UserRole role;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Transient
    @JsonIgnore
    private List<Event> events;

    @ToString.Include(name = "password")
    private String maskPassword() {
        return "********";
    }
}
