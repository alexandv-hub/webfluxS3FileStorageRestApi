package com.example.webfluxS3FileStorageRestApi.rest;

import com.example.webfluxS3FileStorageRestApi.dto.UserDTO;
import com.example.webfluxS3FileStorageRestApi.mapper.UserMapper;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "Operations related to users")
public class UserRestControllerV1 {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/{id}")
    @Operation(summary = "Find a user by ID", description = "Finds a user with the specified ID")
    public Mono<UserDTO> getUserById(@PathVariable Long id, Mono<Authentication> authMono) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (CustomPrincipal) securityContext.getAuthentication().getPrincipal())
                .flatMap(customPrincipal ->
                        userService.getUserByIdAndAuth(id, authMono));
    }

    @GetMapping("/")
    @Operation(summary = "Find all users", description = "Finds all users")
    public Flux<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/")
    @Operation(summary = "Update a user by request body", description = "Updates a user by request body")
    public Mono<UserDTO> updateUser(@RequestBody UserDTO userDTO) {
        return userService.updateUser(userDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user by ID", description = "Deletes a user with the specified ID")
    public Mono<Void> deleteUserById(@PathVariable Long id) {
        return userService.deleteUserById(id);
    }

    @DeleteMapping("/")
    @Operation(summary = "Delete all users!!!", description = "Deletes all users!!!")
    public Mono<Integer> deleteAllUsers() {
        return userService.deleteAllUsers();
    }
}
