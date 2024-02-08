package com.example.webfluxS3FileStorageRestApi.service.impl;

import com.example.webfluxS3FileStorageRestApi.dto.UserDTO;
import com.example.webfluxS3FileStorageRestApi.mapper.UserMapper;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import com.example.webfluxS3FileStorageRestApi.model.UserRole;
import com.example.webfluxS3FileStorageRestApi.repository.UserRepository;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;


    @Test
    void registerUser_Success() {
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";

        UserEntity userEntity = UserEntity.builder()
                .username("testUser")
                .password(rawPassword)
                .build();

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userService.registerUser(userEntity))
                .assertNext(savedUser -> {
                    assert savedUser.getPassword().equals(encodedPassword);
                    assert savedUser.isEnabled();
                    assert savedUser.getRole() == UserRole.USER;
                    assert savedUser.getCreatedAt() != null;
                    assert savedUser.getUpdatedAt() != null;
                })
                .verifyComplete();
    }



    @Test
    void getUserById_Success() {
        Long userId = 1L;

        UserEntity user = new UserEntity();
        when(userRepository.findActiveById(userId)).thenReturn(Mono.just(user));

        StepVerifier.create(userService.getUserById(userId))
                .expectNext(user)
                .verifyComplete();

        verify(userRepository).findActiveById(userId);
    }

    @Test
    void getUserById_NotFound() {
        Long userId = 1L;

        when(userRepository.findActiveById(userId)).thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserById(userId))
                .expectError(ResponseStatusException.class)
                .verify();

        verify(userRepository).findActiveById(userId);
    }

    @Test
    void getUserByIdAndAuth_AsAdminOrModerator_Success() {
        Long userId = 1L;

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        UserEntity user = UserEntity.builder().build();
        user.setId(userId);

        UserDTO userDTO = UserDTO.builder()
                .id(userId)
                .build();

        when(userRepository.findActiveById(userId)).thenReturn(Mono.just(user));
        when(userMapper.map(user)).thenReturn(userDTO);

        StepVerifier.create(userService.getUserByIdAndAuth(userId, Mono.just(authentication)))
                .expectNext(userDTO)
                .verifyComplete();

        verify(userRepository).findActiveById(userId);
    }


    @Test
    void getUserByIdAndAuth_AsUser_NotExistsOrForbidden() {
        Long principalId = 1L;
        Long userIdFind = 2L;

        CustomPrincipal customPrincipal = new CustomPrincipal(principalId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        StepVerifier.create(userService.getUserByIdAndAuth(userIdFind, Mono.just(authentication)))
                .expectError(ResponseStatusException.class)
                .verify();

        verify(userRepository, never()).findActiveById(userIdFind);
    }

    @Test
    void getUserByIdAndAuth_AsUser_Success() {
        Long principalId = 1L;
        Long userIdFind = 1L;

        UserEntity user = new UserEntity();
        user.setId(userIdFind);

        UserDTO userDTO = UserDTO.builder()
                .id(userIdFind)
                .build();

        CustomPrincipal customPrincipal = new CustomPrincipal(principalId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(userRepository.findActiveById(userIdFind)).thenReturn(Mono.just(user));
        when(userMapper.map(user)).thenReturn(userDTO);

        StepVerifier.create(userService.getUserByIdAndAuth(userIdFind, Mono.just(authentication)))
                .expectNext(userDTO)
                .verifyComplete();

        verify(userRepository).findActiveById(userIdFind);
    }



    @Test
    void getUserByUsername_WhenUserExists() {
        String username = "JohnDoe";

        UserEntity user = new UserEntity();
        user.setUsername(username);

        when(userRepository.findActiveByUsername(username)).thenReturn(Mono.just(user));

        StepVerifier.create(userService.getUserByUsername(username))
                .expectNextMatches(foundUser -> foundUser.getUsername().equals(username))
                .verifyComplete();
    }

    @Test
    void getUserByUsername_WhenUserDoesNotExist() {
        String username = "JohnDoe";

        UserEntity user = new UserEntity();
        user.setUsername(username);

        when(userRepository.findActiveByUsername(username)).thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserByUsername(username))
                .expectErrorMatches(error -> error instanceof ResponseStatusException &&
                                             ((ResponseStatusException) error).getStatusCode().equals(HttpStatus.NOT_FOUND) &&
                                             error.getMessage().contains("User with username = '" + username + "' not found"))
                .verify();
    }



    @Test
    void getAllUsers_ReturnsAllUsers() {
        Long user1Id = 1L;
        Long user2Id = 2L;

        UserEntity user1 = UserEntity.builder().build();
        user1.setId(user1Id);

        UserEntity user2 = UserEntity.builder().build();
        user2.setId(user2Id);

        UserDTO userDTO1 = UserDTO.builder()
                .id(user1Id)
                .build();
        UserDTO userDTO2 = UserDTO.builder()
                .id(user2Id)
                .build();

        List<UserEntity> users = List.of(user1, user2);
        List<UserDTO> userDTOs = List.of(userDTO1, userDTO2);

        when(userRepository.findAllActive()).thenReturn(Flux.fromIterable(users));
        when(userMapper.map(user1)).thenReturn(userDTO1);
        when(userMapper.map(user2)).thenReturn(userDTO2);

        StepVerifier.create(userService.getAllUsers())
                .expectNextSequence(userDTOs)
                .verifyComplete();

        verify(userRepository).findAllActive();
    }



    @Test
    void updateUser_UserExistsAndPasswordChanged_UpdatesSuccessfully() {
        UserEntity user = UserEntity.builder()
                .username("existingUser")
                .password("oldPassword")
                .role(UserRole.ADMIN)
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        user.setId(1L);

        UserDTO userDTO = UserDTO.builder()
                .id(1L)
                .username("newUsername")
                .password("newPassword")
                .role(UserRole.ADMIN)
                .firstName("Johny")
                .lastName("Depp")
                .enabled(false)
                .createdAt(user.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findActiveById(userDTO.getId())).thenReturn(Mono.just(user));
        when(userRepository.existsByUsernameAndIdNot(userDTO.getUsername(), userDTO.getId())).thenReturn(Mono.just(false));
        when(passwordEncoder.encode(userDTO.getPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(user));
        when(userMapper.map(any(UserEntity.class))).thenReturn(userDTO);

        StepVerifier.create(userService.updateUser(userDTO))
                .expectNextMatches(updatedUser ->
                        updatedUser.getUsername().equals(userDTO.getUsername()) &&
                        !updatedUser.getPassword().equals(user.getPassword()) &&
                        updatedUser.getRole().equals(userDTO.getRole()) &&
                        updatedUser.getFirstName().equals(userDTO.getFirstName()) &&
                        updatedUser.getLastName().equals(userDTO.getLastName()) &&
                        updatedUser.isEnabled() == userDTO.isEnabled() &&
                        updatedUser.getCreatedAt().equals(user.getCreatedAt()) &&
                        updatedUser.getUpdatedAt().isAfter(user.getCreatedAt())
                        )
                .verifyComplete();
    }

    @Test
    void updateUser_UsernameAlreadyTaken_ReturnsError() {
        UserEntity user = UserEntity.builder().build();
        UserDTO userDTO = UserDTO.builder().build();

        when(userRepository.findActiveById(userDTO.getId())).thenReturn(Mono.just(user));
        when(userRepository.existsByUsernameAndIdNot(userDTO.getUsername(), userDTO.getId())).thenReturn(Mono.just(true));

        StepVerifier.create(userService.updateUser(userDTO))
                .expectErrorMatches(error -> error instanceof ResponseStatusException
                                             && ((ResponseStatusException) error).getStatusCode().equals(HttpStatus.BAD_REQUEST))
                .verify();
    }

    @Test
    void updateUser_UserDoesNotExist_ReturnsError() {
        UserDTO userDTO = UserDTO.builder().build();

        when(userRepository.findActiveById(userDTO.getId())).thenReturn(Mono.empty());

        StepVerifier.create(userService.updateUser(userDTO))
                .expectErrorMatches(error -> error instanceof ResponseStatusException
                                             && ((ResponseStatusException) error).getStatusCode().equals(HttpStatus.NOT_FOUND))
                .verify();
    }



    @Test
    void deleteUserById_WhenUserExists_CompletesSuccessfully() {
        Long userId = 1L;
        when(userRepository.findActiveById(userId)).thenReturn(Mono.just(new UserEntity()));
        when(userRepository.deleteActiveById(userId)).thenReturn(Mono.empty());

        StepVerifier.create(userService.deleteUserById(userId))
                .verifyComplete();

        verify(userRepository).findActiveById(userId);
        verify(userRepository).deleteActiveById(userId);
    }

    @Test
    void deleteUserById_WhenUserDoesNotExist_ThrowsNotFoundException() {
        Long userId = 2L;
        when(userRepository.findActiveById(userId)).thenReturn(Mono.empty());

        StepVerifier.create(userService.deleteUserById(userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode().equals(HttpStatus.NOT_FOUND))
                .verify();

        verify(userRepository).findActiveById(userId);
        verify(userRepository, never()).deleteActiveById(userId);
    }



    @Test
    void deleteAllUsers_SuccessfulDeletion_ReportsCompletion() {
        doReturn(Mono.just(5)).when(userRepository).deleteAllActive();

        StepVerifier.create(userService.deleteAllUsers())
                .expectNext(5)
                .verifyComplete();

        verify(userRepository).deleteAllActive();
    }

    @Test
    void deleteAllUsers_ErrorOccurs_PropagatesError() {
        RuntimeException exception = new RuntimeException("Database error");
        when(userRepository.deleteAllActive()).thenReturn(Mono.error(exception));

        StepVerifier.create(userService.deleteAllUsers())
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                                                 "Database error".equals(throwable.getMessage()))
                .verify();

        verify(userRepository).deleteAllActive();
    }
}
