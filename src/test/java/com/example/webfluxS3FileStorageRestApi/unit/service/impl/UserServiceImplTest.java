package com.example.webfluxS3FileStorageRestApi.unit.service.impl;

import com.example.webfluxS3FileStorageRestApi.dto.*;
import com.example.webfluxS3FileStorageRestApi.mapper.EventMapper;
import com.example.webfluxS3FileStorageRestApi.mapper.UserMapper;
import com.example.webfluxS3FileStorageRestApi.model.Event;
import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import com.example.webfluxS3FileStorageRestApi.model.UserRole;
import com.example.webfluxS3FileStorageRestApi.repository.EventRepository;
import com.example.webfluxS3FileStorageRestApi.repository.FileRepository;
import com.example.webfluxS3FileStorageRestApi.repository.UserRepository;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.impl.UserServiceImpl;
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
    private EventRepository eventRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private UserServiceImpl userService;


    @Test
    void registerUser_Success() {
        String username = "username";
        String firstName = "Alex";
        String lastName = "Petrov";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";

        UserRegisterRequestDTO userRegisterRequestDTO = new UserRegisterRequestDTO(username, rawPassword, firstName, lastName);

        UserEntity userEntity = UserEntity.builder()
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .password(rawPassword)
                .build();

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userMapper.map(eq(userRegisterRequestDTO))).thenReturn(userEntity);

        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userService.registerUser(userRegisterRequestDTO))
                .assertNext(savedUser -> {
                    assert savedUser.getUsername().equals(username);
                    assert savedUser.getFirstName().equals(firstName);
                    assert savedUser.getLastName().equals(lastName);
                    assert savedUser.getPassword().equals(encodedPassword);
                    assert savedUser.isEnabled();
                    assert savedUser.getRole() == UserRole.USER;
                    assert savedUser.getCreatedAt() != null;
                    assert savedUser.getUpdatedAt() == null;
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
        Long fileId = 2L;
        Long eventId = 3L;

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        File file = new File();
        file.setId(fileId);

        Event event = Event.builder()
                .id(eventId)
                .fileId(fileId)
                .build();

        UserEntity user = new UserEntity();
        user.setId(userId);

        EventDTO eventDTO = EventDTO.builder()
                .id(event.getId())
                .build();

        UserDTO userDTO = UserDTO.builder()
                .id(userId)
                .build();

        when(userRepository.findActiveById(userId)).thenReturn(Mono.just(user));
        when(eventRepository.findAllActiveByUserId(userId)).thenReturn(Flux.just(event));
        when(fileRepository.findActiveById(fileId)).thenReturn(Mono.just(file));
        when(eventMapper.map(eq(event), eq(file))).thenReturn(eventDTO);
        when(userMapper.map(user)).thenReturn(userDTO);

        StepVerifier.create(userService.getUserByIdAndAuth(userId, Mono.just(authentication)))
                .expectNextMatches(result ->
                        result.getId().equals(userDTO.getId()) &&
                        result.getEventDTOs().size() == 1 &&
                        result.getEventDTOs().get(0).getId().equals(eventDTO.getId())
                )
                .verifyComplete();

        verify(userRepository).findActiveById(userId);
        verify(eventRepository).findAllActiveByUserId(userId);
        verify(fileRepository).findActiveById(fileId);
        verify(eventMapper).map(eq(event), eq(file));
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
        Long userId = 1L;
        Long fileId = 2L;
        Long eventId = 3L;

        UserEntity user = new UserEntity();
        user.setId(userId);

        Event event = Event.builder()
                .id(eventId)
                .fileId(fileId)
                .build();

        File file = new File();
        file.setId(fileId);

        EventDTO eventDTO = EventDTO.builder()
                .id(eventId)
                .build();

        UserDTO userDTO = UserDTO.builder()
                .id(userId)
                .build();

        CustomPrincipal customPrincipal = new CustomPrincipal(principalId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(userRepository.findActiveById(userId)).thenReturn(Mono.just(user));
        when(eventRepository.findAllActiveByUserId(userId)).thenReturn(Flux.just(event));
        when(fileRepository.findActiveById(fileId)).thenReturn(Mono.just(file));
        when(eventMapper.map(event, file)).thenReturn(eventDTO);
        when(userMapper.map(user)).thenReturn(userDTO);

        StepVerifier.create(userService.getUserByIdAndAuth(userId, Mono.just(authentication)))
                .expectNextMatches(userDto ->
                        userDto.getId().equals(userId) &&
                        userDto.getEventDTOs() != null &&
                        userDto.getEventDTOs().size() == 1 &&
                        userDto.getEventDTOs().get(0).getId().equals(eventDTO.getId())
                )
                .verifyComplete();

        verify(userRepository).findActiveById(userId);
        verify(eventRepository).findAllActiveByUserId(userId);
        verify(fileRepository).findActiveById(fileId);
        verify(eventMapper).map(event, file);
        verify(userMapper).map(user);
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

        List<UserEntity> users = List.of(user1, user2);

        UserBasicDTO userBasicDTO1 = UserBasicDTO.builder().build();
        userBasicDTO1.setId(user1Id);

        UserBasicDTO userBasicDTO2 = UserBasicDTO.builder().build();
        userBasicDTO2.setId(user2Id);

        List<UserBasicDTO> userBasicDTOs = List.of(userBasicDTO1, userBasicDTO2);

        when(userRepository.findAllActive()).thenReturn(Flux.fromIterable(users));
        when(userMapper.mapToUserBasicDTO(user1)).thenReturn(userBasicDTO1);
        when(userMapper.mapToUserBasicDTO(user2)).thenReturn(userBasicDTO2);

        StepVerifier.create(userService.getAllUsers())
                .expectNextSequence(userBasicDTOs)
                .verifyComplete();

        verify(userRepository).findAllActive();
    }



    @Test
    void updateUserById_UserExistsAndPasswordChanged_UpdatesSuccessfully() {
        Long userId = 1L;
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        String existingUserUsername = "existingUserUsername";
        String newUsername = "newUsername";
        String encodedNewPassword = "encodedNewPassword";
        String newFirstName = "Johny";
        String newLastName = "Depp";

        UserEntity existingUser = UserEntity.builder()
                .username(existingUserUsername)
                .password(oldPassword)
                .role(UserRole.ADMIN)
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
        existingUser.setId(userId);

        UserUpdateRequestDTO userUpdateRequestDTO = UserUpdateRequestDTO.builder()
                .password(newPassword)
                .username(newUsername)
                .role(UserRole.USER)
                .firstName(newFirstName)
                .lastName(newLastName)
                .enabled(false)
                .build();

        UserEntity updatedUser = UserEntity.builder()
                .username(newUsername)
                .password(encodedNewPassword)
                .role(UserRole.USER)
                .firstName(newFirstName)
                .lastName(newLastName)
                .enabled(false)
                .createdAt(existingUser.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        updatedUser.setId(userId);

        UserBasicDTO expectedUserBasicDTO = UserBasicDTO.builder()
                .username(newUsername)
                .password(encodedNewPassword)
                .role(UserRole.USER)
                .firstName(newFirstName)
                .lastName(newLastName)
                .enabled(false)
                .createdAt(existingUser.getCreatedAt())
                .updatedAt(updatedUser.getUpdatedAt())
                .build();

        when(userRepository.findActiveById(userId)).thenReturn(Mono.just(existingUser));
        when(userRepository.existsByUsernameAndIdNot(existingUser.getUsername(), userId)).thenReturn(Mono.just(Boolean.FALSE));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(updatedUser));
        when(userMapper.mapToUserBasicDTO(updatedUser)).thenReturn(expectedUserBasicDTO);

        Mono<UserBasicDTO> result = userService.updateUserById(userId, userUpdateRequestDTO);

        StepVerifier.create(result)
                .expectNextMatches(userUpdateRequestDTO1 ->
                        userUpdateRequestDTO1.getUsername().equals(expectedUserBasicDTO.getUsername()) &&
                        userUpdateRequestDTO1.getRole().equals(expectedUserBasicDTO.getRole()) &&
                        userUpdateRequestDTO1.getFirstName().equals(expectedUserBasicDTO.getFirstName()) &&
                        userUpdateRequestDTO1.getLastName().equals(expectedUserBasicDTO.getLastName()) &&
                        userUpdateRequestDTO1.isEnabled() == expectedUserBasicDTO.isEnabled() &&
                        userUpdateRequestDTO1.getCreatedAt().equals(expectedUserBasicDTO.getCreatedAt()) &&
                        userUpdateRequestDTO1.getUpdatedAt().equals(expectedUserBasicDTO.getUpdatedAt()) &&
                        userUpdateRequestDTO1.getPassword().equals(expectedUserBasicDTO.getPassword())
                )
                .verifyComplete();

        verify(userRepository).findActiveById(userId);
        verify(userRepository).existsByUsernameAndIdNot(existingUser.getUsername(), userId);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(UserEntity.class));
        verify(userMapper).mapToUserBasicDTO(any(UserEntity.class));
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
