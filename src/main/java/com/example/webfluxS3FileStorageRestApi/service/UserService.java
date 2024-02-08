package com.example.webfluxS3FileStorageRestApi.service;

import com.example.webfluxS3FileStorageRestApi.dto.UserDTO;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<UserEntity> registerUser(UserEntity user);
    Mono<UserEntity> getUserByUsername(String username);
    Mono<UserEntity> getUserById(Long id);

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    Flux<UserDTO> getAllUsers();

    @PreAuthorize("hasAnyRole('ADMIN')")
    Mono<UserDTO> updateUser(UserDTO userDTO);

    @PreAuthorize("hasAnyRole('ADMIN')")
    Mono<Void> deleteUserById(Long id);

    @PreAuthorize("hasAnyRole('ADMIN')")
    Mono<Integer> deleteAllUsers();

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    Mono<UserDTO> getUserByIdAndAuth(Long id, Mono<Authentication> authMono);
}
