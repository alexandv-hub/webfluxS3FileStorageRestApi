package com.example.webfluxS3FileStorageRestApi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class FileDTO {

    private Long id;
    private String location;
}
