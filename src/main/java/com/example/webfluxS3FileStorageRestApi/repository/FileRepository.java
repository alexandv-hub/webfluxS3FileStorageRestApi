package com.example.webfluxS3FileStorageRestApi.repository;

import com.example.webfluxS3FileStorageRestApi.model.File;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileRepository extends R2dbcRepository<File, Long> {

    @Query("select * from file where status = 'ACTIVE' and id = :id")
    Mono<File> findActiveById(@NonNull Long id);

    @Query("select * from file where status = 'ACTIVE'")
    Flux<File> findAllActive();

    @Query(
            value = """
            SELECT *
            FROM file f
            JOIN event e ON e.file_id = f.id
            WHERE e.user_id = :userId
              AND e.status = 'ACTIVE'
              AND f.status = 'ACTIVE';
            """)
    Flux<File> findAllActiveByUserId(Long userId);

    @Query(
            value = """
            SELECT e.file_id
            FROM event e
            JOIN file f ON e.file_id = f.id
            WHERE SUBSTRING_INDEX(f.location, '/', -1) = :fileName
              AND e.status = 'ACTIVE'
              AND f.status = 'ACTIVE';
            """)
    Mono<Long> getIdByFileName(String fileName);

    @Modifying
    @Query("update file f set status = 'DELETED' where f.id = :id and status = 'ACTIVE'")
    Mono<Void> deleteActiveById(Long id);

    @Modifying
    @Query(
        """
        UPDATE file f
        JOIN event e on e.file_id = f.id
        SET f.status = 'DELETED'
        WHERE e.user_id = :userId
          AND e.status = 'ACTIVE'
          AND f.status = 'ACTIVE';
        """)
    Mono<Integer> deleteAllActiveByUserId(Long userId);

    @Modifying
    @Query("UPDATE file f SET status = 'DELETED' WHERE status = 'ACTIVE'")
    Mono<Integer> deleteAllActive();
}
