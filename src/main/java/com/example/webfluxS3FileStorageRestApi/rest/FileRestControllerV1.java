package com.example.webfluxS3FileStorageRestApi.rest;

import com.example.webfluxS3FileStorageRestApi.dto.FileDTO;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;

import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
@Tag(name = "File", description = "Operations related to files")
public class FileRestControllerV1 {

    private final FileService fileService;

    @GetMapping("/{id}")
    public Mono<EntityModel<FileDTO>> getFileById(@PathVariable Long id, Mono<Authentication> authMono) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (CustomPrincipal) securityContext.getAuthentication().getPrincipal())
                .flatMap(customPrincipal ->
                        fileService.getFileByIdAndAuth(id, authMono)
                                .flatMap(fileDTO -> buildEntityModelWithLinks(fileDTO, authMono)));
    }

    @GetMapping(path = "/")
    @Operation(summary = "Find all files", description = "Finds all files")
    public Flux<EntityModel<FileDTO>> getAllFiles(Mono<Authentication> authMono) {
        return fileService.getAllFiles(authMono)
                .flatMap(fileDTO -> buildEntityModelWithLinks(fileDTO, authMono));
    }

    @PutMapping(path = "/")
    @Operation(summary = "Update a file by request body", description = "Updates a file by request body")
    public Mono<FileDTO> updateFile(@RequestBody FileDTO fileDTO) {
        return fileService.updateFile(fileDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file by ID", description = "Deletes a file with the specified ID")
    public Mono<Void> deleteFileById(@PathVariable Long id) {
        return fileService.deleteFileById(id);
    }

    @DeleteMapping("/")
    @Operation(summary = "Delete all files by user ID", description = "Deletes all files by user ID")
    public Mono<Integer> deleteAllFilesByUserId(@RequestParam Long userId) {
        return fileService.deleteAllFilesByUserId(userId);
    }

    @DeleteMapping("/all")
    @Operation(summary = "Delete all files!!!", description = "Deletes all files!!!")
    public Mono<Integer> deleteAllFiles() {
        return fileService.deleteAllFiles();
    }

    private Mono<EntityModel<FileDTO>> buildEntityModelWithLinks(FileDTO fileDTO, Mono<Authentication> authMono) {
        Mono<Link> selfLinkMono = linkTo(
                methodOn(FileRestControllerV1.class).getFileById(fileDTO.getId(), authMono)).withSelfRel().toMono();

        String fileName = Paths.get(fileDTO.getLocation()).getFileName().toString();
        Mono<Link> downloadLinkMono = linkTo(
                methodOn(FileStorageRestControllerV1.class).downloadFileByName(fileName, authMono)).withRel("download").toMono();

        return Mono.zip(selfLinkMono, downloadLinkMono)
                .map(links -> EntityModel.of(fileDTO, links.getT1(), links.getT2()));
    }
}
