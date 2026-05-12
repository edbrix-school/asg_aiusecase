package com.asg.aiusecase.repository;

import com.asg.aiusecase.entity.LlmResolutionCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface LlmResolutionCacheRepository extends JpaRepository<LlmResolutionCacheEntity, Long> {

    Optional<LlmResolutionCacheEntity> findFirstByAmbiguityHashAndModelAndExpiresAtAfter(String ambiguityHash,
                                                                                         String model,
                                                                                         OffsetDateTime now);
}
