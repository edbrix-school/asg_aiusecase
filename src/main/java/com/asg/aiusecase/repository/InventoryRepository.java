package com.asg.aiusecase.repository;

import com.asg.aiusecase.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findFirstByProductCodeIgnoreCase(String productCode);

    Optional<InventoryEntity> findFirstByStockCodeIgnoreCase(String stockCode);

    boolean existsByProductCodeIgnoreCase(String productCode);

    boolean existsByStockCodeIgnoreCase(String stockCode);
}
