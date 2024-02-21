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
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "file.status", ignore = true)
    Event map(EventDTO eventDTO);

    @Mapping(source = "event.id", target = "id")
    @Mapping(source = "file", target = "file")
    EventDTO map(Event event, File file);
}
