package com.example.webfluxS3FileStorageRestApi.service.impl;

import com.example.webfluxS3FileStorageRestApi.dto.FileDTO;
import com.example.webfluxS3FileStorageRestApi.mapper.FileMapper;
import com.example.webfluxS3FileStorageRestApi.model.Event;
import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.repository.EventRepository;
import com.example.webfluxS3FileStorageRestApi.repository.FileRepository;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private FileServiceImpl fileService;

    @Captor
    private ArgumentCaptor<File> fileCaptor;


    @Test
    void getFileByIdAndAuth_AsAdminOrModerator_Success() {
        Long fileId = 1L;
        Long userId = 1L;
        String location = "/path/location/test.txt";

        File file = File.builder()
                .id(fileId)
                .location(location)
                .build();

        FileDTO fileDTO = FileDTO.builder()
                .id(fileId)
                .location(location)
                .build();

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(fileRepository.findActiveById(fileId)).thenReturn(Mono.just(file));
        when(fileMapper.map(file)).thenReturn(fileDTO);

        StepVerifier.create(fileService.getFileByIdAndAuth(fileId, Mono.just(authentication)))
                .expectNext(fileDTO)
                .verifyComplete();

        verify(fileRepository).findActiveById(fileId);
        verify(eventRepository, never()).findActiveByFileIdAndUserId(fileId, userId);
    }

    @Test
    void getFileByIdAndAuth_AsUser_NotExistsOrForbidden() {
        Long fileId = 1L;
        Long userId = 1L;

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(eventRepository.findActiveByFileIdAndUserId(fileId, userId)).thenReturn(Mono.empty());

        StepVerifier.create(fileService.getFileByIdAndAuth(fileId, Mono.just(authentication)))
                .expectError(ResponseStatusException.class)
                .verify();

        verify(eventRepository).findActiveByFileIdAndUserId(fileId, userId);
        verify(eventRepository, never()).findActiveById(fileId);
    }

    @Test
    void getFileByIdAndAuth_AsUser_Success() {
        Long fileId = 1L;
        Long userId = 1L;
        String location = "/path/location/test.txt";

        File file = File.builder()
                .id(fileId)
                .location(location)
                .build();

        FileDTO fileDTO = FileDTO.builder()
                .id(fileId)
                .location(location)
                .build();

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(eventRepository.findActiveByFileIdAndUserId(fileId, userId)).thenReturn(Mono.just(new Event()));
        when(fileRepository.findActiveById(fileId)).thenReturn(Mono.just(file));
        when(fileMapper.map(file)).thenReturn(fileDTO);

        StepVerifier.create(fileService.getFileByIdAndAuth(fileId, Mono.just(authentication)))
                .expectNext(fileDTO)
                .verifyComplete();

        verify(eventRepository).findActiveByFileIdAndUserId(fileId, userId);
        verify(fileRepository).findActiveById(fileId);
    }



    @Test
    void getAllFiles_AsAdminOrModerator_ReturnsAllFiles() {
        Long userId = 1L;
        File file1 = new File();
        File file2 = new File();

        FileDTO fileDTO1 = FileDTO.builder().build();
        FileDTO fileDTO2 = FileDTO.builder().build();

        List<File> files = List.of(file1, file2);
        List<FileDTO> fileDTOs = List.of(fileDTO1, fileDTO2);

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(fileRepository.findAllActive()).thenReturn(Flux.fromIterable(files));
        when(fileMapper.map(file1)).thenReturn(fileDTO1);
        when(fileMapper.map(file2)).thenReturn(fileDTO2);

        StepVerifier.create(fileService.getAllFiles(Mono.just(authentication)))
                .expectNextSequence(fileDTOs)
                .verifyComplete();

        verify(fileRepository, never()).findAllActiveByUserId(userId);
        verify(fileRepository).findAllActive();
    }

    @Test
    void getAllFiles_AsUser_ReturnsUserFiles() {
        Long userId = 1L;

        File file1 = new File();
        File file2 = new File();

        FileDTO fileDTO1 = FileDTO.builder().build();
        FileDTO fileDTO2 = FileDTO.builder().build();

        List<File> userFiles = List.of(file1, file2);
        List<FileDTO> userFileDTOs = List.of(fileDTO1, fileDTO2);

        CustomPrincipal customPrincipal = new CustomPrincipal(userId, "John Doe");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(customPrincipal);
        doReturn(authorities).when(authentication).getAuthorities();

        when(fileRepository.findAllActiveByUserId(userId)).thenReturn(Flux.fromIterable(userFiles));
        when(fileMapper.map(file1)).thenReturn(fileDTO1);
        when(fileMapper.map(file2)).thenReturn(fileDTO2);

        StepVerifier.create(fileService.getAllFiles(Mono.just(authentication)))
                .expectNextSequence(userFileDTOs)
                .verifyComplete();

        verify(fileRepository, never()).findAllActive();
        verify(fileRepository).findAllActiveByUserId(userId);
    }



    @Test
    void updateFile_FileExists_UpdatesSuccessfully() {
        Long fileId = 1L;
        String originalLocation = "/path/original/location.txt";
        String updatedLocation = "/path/updated/location.txt";

        File file = File.builder()
                .id(fileId)
                .location(originalLocation)
                .build();

        FileDTO fileDTO = FileDTO.builder()
                .id(fileId)
                .location(updatedLocation)
                .build();

        FileDTO updatedFileDTO = FileDTO.builder()
                .id(fileId)
                .location(updatedLocation)
                .build();

        when(fileRepository.findActiveById(fileId)).thenReturn(Mono.just(file));
        when(fileRepository.save(any(File.class))).thenReturn(Mono.just(file));
        when(fileMapper.map(any(File.class))).thenReturn(updatedFileDTO);

        StepVerifier.create(fileService.updateFile(fileDTO))
                .expectNext(updatedFileDTO)
                .verifyComplete();

        verify(fileRepository).save(fileCaptor.capture());
        File savedFile = fileCaptor.getValue();
        assertEquals(updatedLocation, savedFile.getLocation());
    }

    @Test
    void updateFile_FileDoesNotExist_ThrowsException() {
        Long fileId = 1L;
        String location = "/path/updated/location.txt";

        File file = new File();
        FileDTO fileDTO = FileDTO.builder()
                .id(fileId)
                .location(location)
                .build();

        when(fileRepository.findActiveById(fileId)).thenReturn(Mono.empty());

        StepVerifier.create(fileService.updateFile(fileDTO))
                .expectError(ResponseStatusException.class)
                .verify();

        verify(fileRepository, never()).save(file);
        verify(fileRepository).findActiveById(fileId);
    }



    @Test
    void deleteFileById_WhenFileExists_CompletesSuccessfully() {
        Long fileId = 1L;
        when(fileRepository.findActiveById(fileId)).thenReturn(Mono.just(new File()));
        when(fileRepository.deleteActiveById(fileId)).thenReturn(Mono.empty());

        StepVerifier.create(fileService.deleteFileById(fileId))
                .verifyComplete();

        verify(fileRepository).findActiveById(fileId);
        verify(fileRepository).deleteActiveById(fileId);
    }

    @Test
    void deleteFileById_WhenFileDoesNotExist_ThrowsNotFoundException() {
        Long fileId = 2L;
        when(fileRepository.findActiveById(fileId)).thenReturn(Mono.empty());

        StepVerifier.create(fileService.deleteFileById(fileId))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode().equals(HttpStatus.NOT_FOUND))
                .verify();

        verify(fileRepository).findActiveById(fileId);
        verify(fileRepository, never()).deleteActiveById(fileId);
    }



    @Test
    void deleteAllFilesByUserId_SuccessfulDeletion_ReturnsCount() {
        Long userId = 1L;
        doReturn(Mono.just(3)).when(fileRepository).deleteAllActiveByUserId(userId);

        StepVerifier.create(fileService.deleteAllFilesByUserId(userId))
                .expectNext(3)
                .verifyComplete();

        verify(fileRepository).deleteAllActiveByUserId(userId);
    }

    @Test
    void deleteAllFilesByUserId_ErrorOccurs_PropagatesError() {
        Long userId = 2L;
        RuntimeException exception = new RuntimeException("Database error");
        when(fileRepository.deleteAllActiveByUserId(userId)).thenReturn(Mono.error(exception));

        StepVerifier.create(fileService.deleteAllFilesByUserId(userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                                 throwable.getMessage().equals("Database error"))
                .verify();

        verify(fileRepository).deleteAllActiveByUserId(userId);
    }



    @Test
    void deleteAllFiles_SuccessfulDeletion_ReportsCompletion() {
        doReturn(Mono.just(5)).when(fileRepository).deleteAllActive();

        StepVerifier.create(fileService.deleteAllFiles())
                .expectNext(5)
                .verifyComplete();

        verify(fileRepository).deleteAllActive();
    }

    @Test
    void deleteAllFiles_ErrorOccurs_PropagatesError() {
        RuntimeException exception = new RuntimeException("Database error");
        when(fileRepository.deleteAllActive()).thenReturn(Mono.error(exception));

        StepVerifier.create(fileService.deleteAllFiles())
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                                                 "Database error".equals(throwable.getMessage()))
                .verify();

        verify(fileRepository).deleteAllActive();
    }
}
