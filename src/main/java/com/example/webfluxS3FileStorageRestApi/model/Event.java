package com.example.webfluxS3FileStorageRestApi.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Table(name = "event")
public class Event extends BaseEntity {

    private Long userId;
    private Long fileId;

    @Transient
    private UserEntity user;

    @Transient
    private File file;
}
