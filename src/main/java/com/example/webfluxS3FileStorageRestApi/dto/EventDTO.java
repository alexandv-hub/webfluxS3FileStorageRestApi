package com.example.webfluxS3FileStorageRestApi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventDTO {

    private Long id;
    private Long userId;
    private Long fileId;

    @JsonProperty("file")
    private FileDTO fileDTO;
}
