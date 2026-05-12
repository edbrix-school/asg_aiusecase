package com.asg.aiusecase.repository;

import com.asg.aiusecase.entity.CompatibilityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRuleEntity, Long> {

    Optional<CompatibilityRuleEntity> findFirstByInventoryIdAndUnitId(Long inventoryId, Long unitId);

    List<CompatibilityRuleEntity> findByInventoryIdAndValidTrueOrderByPriorityAsc(Long inventoryId);
}
