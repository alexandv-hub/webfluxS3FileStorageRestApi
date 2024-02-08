package com.example.webfluxS3FileStorageRestApi.service.impl;

import com.example.webfluxS3FileStorageRestApi.dto.UserDTO;
import com.example.webfluxS3FileStorageRestApi.mapper.UserMapper;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import com.example.webfluxS3FileStorageRestApi.model.UserRole;
import com.example.webfluxS3FileStorageRestApi.repository.UserRepository;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.ERR_ACCESS_DENIED;
import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.Users.*;
import static com.example.webfluxS3FileStorageRestApi.messages.InfoMessages.Users.*;
import static com.example.webfluxS3FileStorageRestApi.security.SecurityUtils.isAdminOrModerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public Mono<UserEntity> registerUser(UserEntity user) {
        log.info("IN UserServiceImpl registerUser: {}", user);
        return userRepository.save(
                user.toBuilder()
                        .password(passwordEncoder.encode(user.getPassword()))
                        .role(UserRole.USER)
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        ).doOnSuccess(u -> log.info(INFO_IN_REGISTER_USER_USER_CREATED, u));
    }

    @Override
    public Mono<UserEntity> getUserById(Long id) {
        log.info("IN UserServiceImpl getUserById {}", id);
        return userRepository.findActiveById(id)
                .doOnSuccess(aVoid -> log.info(INFO_FOUND_SUCCESSFULLY_USER_WITH_ID, id))
                .doOnError(error -> log.error(ERR_FIND_USER_WITH_ID, id, error.getMessage()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getUserWithIdNotFoundStr(id))));
    }

    @Override
    public Mono<UserDTO> getUserByIdAndAuth(Long id, Mono<Authentication> authMono) {
        log.info("IN UserServiceImpl getUserByIdAndAuth: {}", id);
        return authMono
                .flatMap(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMap(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return userRepository.findActiveById(id);
                                } else {
                                    return principal.getId().equals(id) ?
                                            userRepository.findActiveById(id) :
                                            Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_ACCESS_DENIED));
                                }
                            })
                            .map(userMapper::map);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getUserWithIdNotFoundStr(id))))
                .doOnSuccess(unused -> log.info(INFO_USER_FOUND_SUCCESSFULLY_WITH_ID, id))
                .doOnError(error -> log.error(ERR_FIND_USER_WITH_ID, id, error.getMessage()));
    }

    @Override
    public Mono<UserEntity> getUserByUsername(String username) {
        log.info("IN UserServiceImpl getUserByUsername {}", username);
        return userRepository.findActiveByUsername(username)
                .doOnSuccess(aVoid -> log.info(INFO_FOUND_SUCCESSFULLY_USER_WITH_USERNAME, username))
                .doOnError(error -> log.error(ERR_FIND_USER_WITH_USERNAME, username, error.getMessage()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getUserWithUsernameNotFoundStr(username))));
    }

    @Override
    public Flux<UserDTO> getAllUsers() {
        log.info("IN UserServiceImpl getAllUsers");
        return userRepository.findAllActive()
                .map(userMapper::map);
    }

    @Override
    public Mono<UserDTO> updateUser(UserDTO userDTO) {
        log.info("IN UserServiceImpl updateUser {}", userDTO);
        Long userDTOId = userDTO.getId();
        return userRepository.findActiveById(userDTOId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getUserWithIdNotFoundStr(userDTOId))))
                .flatMap(user -> userRepository.existsByUsernameAndIdNot(userDTO.getUsername(), userDTOId)
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_USERNAME_ALREADY_TAKEN));
                            }
                            userMapper.updateUserFromDto(userDTO, user);
                            if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                                user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                            }
                            user.setUpdatedAt(LocalDateTime.now());
                            return userRepository.save(user);
                        }))
                .map(userMapper::map)
                .doOnSuccess(aVoid -> log.info(INFO_USER_UPDATED_SUCCESSFULLY_WITH_ID, userDTOId))
                .doOnError(error -> log.error(ERR_UPDATING_USER_WITH_ID, userDTO, error.getMessage()));
    }

    @Override
    public Mono<Void> deleteUserById(Long id) {
        log.info("IN UserServiceImpl deleteUserById '{}'", id);
        return userRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, getUserWithIdNotFoundStr(id))))
                .flatMap(user -> {
                    log.info(INFO_DELETING_USER_WITH_ID, id);
                    return userRepository.deleteActiveById(id);
                })
                .then()
                .doOnSuccess(aVoid -> log.info(INFO_FILE_DELETED_SUCCESSFULLY_WITH_ID, id))
                .doOnError(error -> log.error(ERR_DELETING_USER_WITH_ID, error.getMessage()));
    }

    @Override
    public Mono<Integer> deleteAllUsers() {
        log.info("IN UserServiceImpl deleteAllUsers");
        return userRepository.deleteAllActive()
                .doOnTerminate(() -> log.info(INFO_ALL_USERS_DELETED_SUCCESSFULLY))
                .doOnError(error -> log.error(ERR_DELETING_ALL_USERS, error.getMessage()));
    }

    private static String getUserWithIdNotFoundStr(Long id) {
        return "User with ID = '" + id + "' not found";
    }

    private static String getUserWithUsernameNotFoundStr(String username) {
        return "User with username = '" + username + "' not found";
    }
}
