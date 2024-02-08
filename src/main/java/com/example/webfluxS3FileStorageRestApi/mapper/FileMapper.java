package com.example.webfluxS3FileStorageRestApi.mapper;

import com.example.webfluxS3FileStorageRestApi.dto.FileDTO;
import com.example.webfluxS3FileStorageRestApi.model.File;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMapper {

    FileDTO map(File user);

    @InheritInverseConfiguration
    @Mapping(target = "status", ignore = true)
    File map(FileDTO dto);
}
