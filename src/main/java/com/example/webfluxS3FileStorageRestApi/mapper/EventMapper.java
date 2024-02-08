package com.example.webfluxS3FileStorageRestApi.mapper;

import com.example.webfluxS3FileStorageRestApi.dto.EventDTO;
import com.example.webfluxS3FileStorageRestApi.model.Event;
import com.example.webfluxS3FileStorageRestApi.model.File;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventDTO map(Event event);

    @InheritInverseConfiguration
    @Mapping(target = "user", ignore = true)
    Event map(EventDTO dto);

    @Mapping(source = "event.id", target = "id")
    @Mapping(source = "file.id", target = "fileDTO.id")
    @Mapping(source = "file.location", target = "fileDTO.location")
    EventDTO map(Event event, File file);
}
