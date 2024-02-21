package com.example.webfluxS3FileStorageRestApi.repository;

import com.example.webfluxS3FileStorageRestApi.model.Event;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventRepository extends R2dbcRepository<Event, Long> {

    @Query("select * from event where status = 'ACTIVE' and id = :id")
    Mono<Event> findActiveById(Long id);

    @Query("select * from event where status = 'ACTIVE'")
    Flux<Event> findAllActive();

    @Query("select * from event where status = 'ACTIVE' and user_id = :userId")
    Flux<Event> findAllActiveByUserId(Long userId);

    @Query("select * from event where status = 'ACTIVE' and id = :id and user_id = :userId")
    Mono<Event> findActiveByIdAndUserId(Long id, Long userId);

    @Query("select * from event where status = 'ACTIVE' and file_id = :fileId and user_id = :userId")
    Mono<Event> findActiveByFileIdAndUserId(Long fileId, Long userId);

    @Modifying
    @Query("update event set status = 'DELETED' where status = 'ACTIVE' and id = :id")
    Mono<Void> deleteActiveById(Long id);

    @Modifying
    @Query("update event set status = 'DELETED' where status = 'ACTIVE' and user_id = :userId")
    Mono<Integer> deleteAllActiveByUserId(Long userId);

    @Modifying
    @Query("update event set status = 'DELETED' where status = 'ACTIVE'")
    Mono<Integer> deleteAllActive();
}
