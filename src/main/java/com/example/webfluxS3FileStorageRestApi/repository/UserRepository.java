package com.example.webfluxS3FileStorageRestApi.repository;

import com.example.webfluxS3FileStorageRestApi.model.UserEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<UserEntity, Long> {

    Mono<Boolean> existsByUsernameAndIdNot(String username, Long id);

    @Query("select * from users where status = 'ACTIVE' and username = :username")
    Mono<UserEntity> findActiveByUsername(String username);

    @Query("select * from users where status = 'ACTIVE' and id = :id")
    Mono<UserEntity> findActiveById(Long id);

    @Query("select * from users where status = 'ACTIVE'")
    Flux<UserEntity> findAllActive();

    @Modifying
    @Query("update users u set status = 'DELETED' where status = 'ACTIVE' and u.id = :id")
    Mono<Void> deleteActiveById(Long id);

    @Modifying
    @Query("update users set status = 'DELETED' where status = 'ACTIVE'")
    Mono<Integer> deleteAllActive();
}
