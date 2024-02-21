package com.example.webfluxS3FileStorageRestApi.mapper;

import com.example.webfluxS3FileStorageRestApi.dto.EventBasicDTO;
import com.example.webfluxS3FileStorageRestApi.model.Event;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventUpdateDTOMapper {

    EventBasicDTO map(Event event);

    @InheritInverseConfiguration
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "file", ignore = true)
    Event map(EventBasicDTO eventBasicDTO);
}
