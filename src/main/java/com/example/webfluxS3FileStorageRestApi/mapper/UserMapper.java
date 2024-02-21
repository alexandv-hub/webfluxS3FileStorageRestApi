package com.example.webfluxS3FileStorageRestApi.mapper;

import com.example.webfluxS3FileStorageRestApi.dto.UserBasicDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserRegisterRequestDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserUpdateRequestDTO;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "eventDTOs", ignore = true)
    UserDTO map(UserEntity user);

    @InheritInverseConfiguration
    @Mapping(target = "events", ignore = true)
    UserEntity map(UserDTO userDTO);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "events", ignore = true)
    UserEntity map(UserRegisterRequestDTO userRegisterRequestDTO);

    @Mapping(target = "events", ignore = true)
    UserEntity map(UserBasicDTO userBasicDTO);

    UserBasicDTO mapToUserBasicDTO(UserEntity userEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUserEntityFromUserUpdateRequestDTO(UserUpdateRequestDTO userUpdateRequestDTO, @MappingTarget UserEntity userEntity);
}
