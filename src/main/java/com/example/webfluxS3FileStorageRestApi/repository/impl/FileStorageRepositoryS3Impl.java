package com.example.webfluxS3FileStorageRestApi.repository.impl;

import com.example.webfluxS3FileStorageRestApi.repository.FileStorageRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class FileStorageRepositoryS3Impl implements FileStorageRepository {

    private static final Region AWS_S3_REGION_EU_CENTRAL_1 = Region.EU_CENTRAL_1;

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    @Value("${app.s3.key-prefix}")
    private String keyPrefix;

    @Value("${app.s3.aws-access-key-id}")
    private String awsAccessKeyId;

    @Value("${app.s3.aws-secret-access-key}")
    private String awsSecretAccessKey;

    private S3AsyncClient s3Client;

    @PostConstruct
    public void init() {
        this.s3Client = S3AsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(this.awsAccessKeyId, this.awsSecretAccessKey)))
                .region(AWS_S3_REGION_EU_CENTRAL_1)
                .build();
    }

    @Override
    public Mono<Void> uploadUserFileToStorage(FilePart filePart) {
        String fileName = filePart.filename();
        Path tempDir = Paths.get("/tmp/myapp");
        Path tempFile = tempDir.resolve("file-" + fileName);

        return Mono.fromCallable(() -> {
                    try {
                        Files.createDirectories(tempDir);
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException("Create temp directory failed: " + tempDir, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then(filePart.transferTo(tempFile))
                .doOnSuccess(aVoid -> log.info("File saved successfully: " + tempFile))
                .then(Mono.fromFuture(() ->
                        s3Client.putObject(PutObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(keyPrefix + "/" + fileName)
                                        .build(),
                                AsyncRequestBody.fromFile(tempFile.toFile()))
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(aVoid -> log.info("File uploaded successfully to S3: " + bucketName))
                .doOnError(error -> log.error("File upload to S3 failed: " + error.getMessage()))
                .then();
    }

    @Override
    public Mono<ResponseEntity<Resource>> downloadFileFromStorage(String fileName) {
        String key = keyPrefix + "/" + fileName;
        return Mono.fromFuture(() ->
                        s3Client.getObject(
                                GetObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(key)
                                        .build(),
                                AsyncResponseTransformer.toBytes()))
                .map(responseBytes ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .body((Resource) new ByteArrayResource(responseBytes.asByteArray()))
                )
                .doOnSuccess(aVoid -> log.info("File downloaded successfully from S3: " + fileName))
                .doOnError(error -> log.error("File downloaded from S3 failed: " + fileName, error));
    }
}
