package com.example.webfluxS3FileStorageRestApi.dto;

import com.example.webfluxS3FileStorageRestApi.model.File;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventDTO {

    private Long id;
    private Long userId;
    private Long fileId;
    private File file;
}
