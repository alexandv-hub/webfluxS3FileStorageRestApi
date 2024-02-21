package com.example.webfluxS3FileStorageRestApi.rest;

import com.example.webfluxS3FileStorageRestApi.model.File;
import com.example.webfluxS3FileStorageRestApi.security.CustomPrincipal;
import com.example.webfluxS3FileStorageRestApi.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(summary = "Find a file by ID", description = "Finds a file with the specified ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    public Mono<EntityModel<File>> getFileById(@PathVariable Long id, Mono<Authentication> authMono) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (CustomPrincipal) securityContext.getAuthentication().getPrincipal())
                .flatMap(customPrincipal ->
                        fileService.getFileByIdAndAuth(id, authMono)
                                .flatMap(file -> buildEntityModelWithLinks(file, authMono)));
    }

    @GetMapping(path = "/")
    @Operation(summary = "Find all files or files by user ID if role USER", description = "Finds all files or files by user ID if role USER")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    public Flux<EntityModel<File>> getAllFiles(Mono<Authentication> authMono) {
        return fileService.getAllFilesByAuth(authMono)
                .flatMap(file -> buildEntityModelWithLinks(file, authMono));
    }

    @GetMapping("/by-user-id/")
    @Operation(summary = "Find all files by user ID", description = "Finds all files by user ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public Flux<EntityModel<File>> getAllFilesByUserId(@RequestParam Long userId, Mono<Authentication> authMono) {
        return fileService.getFilesByUserId(userId)
                .flatMap(fileDTO -> buildEntityModelWithLinks(fileDTO, authMono));
    }

    @PutMapping(path = "/{id}")
    @Operation(summary = "Update a file by request body", description = "Updates a file by request body")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public Mono<File> updateFile(@PathVariable Long id, @RequestBody File file) {
        return fileService.updateFileById(id, file);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file by ID", description = "Deletes a file with the specified ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public Mono<Void> deleteFileById(@PathVariable Long id) {
        return fileService.deleteFileById(id);
    }

    @DeleteMapping("/")
    @Operation(summary = "Delete all files by user ID", description = "Deletes all files by user ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public Mono<Integer> deleteAllFilesByUserId(@RequestParam Long userId) {
        return fileService.deleteAllFilesByUserId(userId);
    }

    @DeleteMapping("/all")
    @Operation(summary = "Delete all files!!!", description = "Deletes all files!!!")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Mono<Integer> deleteAllFiles() {
        return fileService.deleteAllFiles();
    }

    private Mono<EntityModel<File>> buildEntityModelWithLinks(File file, Mono<Authentication> authMono) {
        Mono<Link> selfLinkMono = linkTo(
                methodOn(FileRestControllerV1.class).getFileById(file.getId(), authMono)).withSelfRel().toMono();

        String fileName = Paths.get(file.getLocation()).getFileName().toString();
        Mono<Link> downloadLinkMono = linkTo(
                methodOn(FileStorageRestControllerV1.class).downloadFileByName(fileName, authMono)).withRel("download").toMono();

        return Mono.zip(selfLinkMono, downloadLinkMono)
                .map(links -> EntityModel.of(file, links.getT1(), links.getT2()));
    }
}
