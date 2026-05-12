package com.asg.aiusecase.repository;

import com.asg.aiusecase.entity.SynonymEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SynonymRepository extends JpaRepository<SynonymEntity, Long> {

    Optional<SynonymEntity> findFirstByTypeIgnoreCaseAndSynonymIgnoreCase(String type, String synonym);

    List<SynonymEntity> findByTypeIgnoreCase(String type);
}
