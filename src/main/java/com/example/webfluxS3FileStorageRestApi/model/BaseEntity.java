package com.example.webfluxS3FileStorageRestApi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@RequiredArgsConstructor
public abstract class BaseEntity {

    @Id
    protected Long id;

    @JsonIgnore
    protected Status status;
}
