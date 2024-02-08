package com.example.webfluxS3FileStorageRestApi.model;

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
    protected Status status;
}
