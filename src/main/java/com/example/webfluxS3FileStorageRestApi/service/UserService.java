package com.example.webfluxS3FileStorageRestApi.service;

import com.example.webfluxS3FileStorageRestApi.dto.UserBasicDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserRegisterRequestDTO;
import com.example.webfluxS3FileStorageRestApi.dto.UserUpdateRequestDTO;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<UserEntity> registerUser(UserRegisterRequestDTO user);
    Mono<UserEntity> getUserByUsername(String username);
    Mono<UserEntity> getUserById(Long id);

    Flux<UserBasicDTO> getAllUsers();

    Mono<UserBasicDTO> updateUserById(Long id, UserUpdateRequestDTO userUpdateRequestDTO);

    Mono<Void> deleteUserById(Long id);

    Mono<Integer> deleteAllUsers();

    Mono<UserDTO> getUserByIdAndAuth(Long id, Mono<Authentication> authMono);
}
