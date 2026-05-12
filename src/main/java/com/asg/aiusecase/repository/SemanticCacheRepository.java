package com.asg.aiusecase.repository;

import com.asg.aiusecase.entity.SemanticCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SemanticCacheRepository extends JpaRepository<SemanticCacheEntity, Long> {

    Optional<SemanticCacheEntity> findFirstByQueryHashAndExpiresAtAfter(String queryHash, OffsetDateTime now);
}
