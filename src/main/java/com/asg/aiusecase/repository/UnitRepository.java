package com.asg.aiusecase.repository;

import com.asg.aiusecase.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<UnitEntity, Long> {

    List<UnitEntity> findByInventoryId(Long inventoryId);

    Optional<UnitEntity> findFirstByInventoryIdAndUnitCodeIgnoreCase(Long inventoryId, String unitCode);

    Optional<UnitEntity> findFirstByUnitCodeIgnoreCase(String unitCode);

    Optional<UnitEntity> findFirstByUnitNameIgnoreCase(String unitName);
}
