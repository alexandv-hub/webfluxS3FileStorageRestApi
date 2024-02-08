package com.example.webfluxS3FileStorageRestApi.mapper;

import com.example.webfluxS3FileStorageRestApi.dto.UserDTO;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO map(UserEntity user);

    @InheritInverseConfiguration
    @Mapping(target = "events", ignore = true)
    UserEntity map(UserDTO dto);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "events", ignore = true)
    void updateUserFromDto(UserDTO dto, @MappingTarget UserEntity entity);
}
