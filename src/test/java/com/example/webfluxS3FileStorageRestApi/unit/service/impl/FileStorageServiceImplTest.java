package com.example.webfluxS3FileStorageRestApi.unit.service.impl;

import com.example.webfluxS3FileStorageRestApi.dto.UploadedFileResponseDTO;
import com.example.webfluxS3FileStorageRestApi.model.Event;
import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import com.example.webfluxS3FileStorageRestApi.repository.EventRepository;
import com.example.webfluxS3FileStorageRestApi.repository.FileRepository;
import com.example.webfluxS3FileStorageRestApi.repository.FileStorageRepository;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    @Mock
    private FileStorageRepository fileStorageRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @Test
    void uploadUserFileToStorageTest() {
        Long userId = 1L;
        String fileName = "testFile.txt";
        FilePart filePart = Mockito.mock(FilePart.class);
        UserEntity user = new UserEntity();
        user.setId(userId);

        File file = File.builder()
                .location("https://bucket-name.s3.amazonaws.com/" + fileName)
                .build();

        Event event = new Event();
        UploadedFileResponseDTO uploadedFileResponseDTO = new UploadedFileResponseDTO(fileName, LocalDateTime.now());

        when(filePart.filename()).thenReturn(fileName);
        when(authentication.getPrincipal()).thenReturn(new CustomPrincipal(userId, "username"));
        Mono<Authentication> authMono = Mono.just(authentication);

        when(fileRepository.save(any(File.class))).thenReturn(Mono.just(file));
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(event));
        when(fileStorageRepository.uploadUserFileToStorage(filePart)).thenReturn(Mono.just(uploadedFileResponseDTO));

        StepVerifier.create(fileStorageService.uploadUserFileToStorage(filePart, authMono))
                .expectNext(uploadedFileResponseDTO)
                .verifyComplete();
    }

    @Test
    void downloadUserFileFromStorageTest() {
        String fileName = "testFile.txt";
        Resource resource = Mockito.mock(Resource.class);
        Long userId = 1L;

        when(authentication.getPrincipal()).thenReturn(new CustomPrincipal(userId, "username"));
        Mono<Authentication> authMono = Mono.just(authentication);

        when(fileStorageRepository.downloadFileFromStorage(fileName))
                .thenReturn(Mono.just(ResponseEntity.ok().body(resource)));

        StepVerifier.create(fileStorageService.downloadFileFromStorageByFileNameAndAuth(fileName, authMono))
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.OK &&
                        Objects.equals(response.getBody(), resource))
                .verifyComplete();
    }
}
