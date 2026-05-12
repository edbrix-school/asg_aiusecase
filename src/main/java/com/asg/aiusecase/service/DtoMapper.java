package com.asg.aiusecase.service;

import com.asg.aiusecase.businesslogic.ResolvedCandidate;
import com.asg.aiusecase.dto.CandidateDto;
import com.asg.aiusecase.dto.InventoryDto;
import com.asg.aiusecase.dto.MatchedStockDto;
import com.asg.aiusecase.dto.UnitDto;
import com.asg.aiusecase.entity.CompatibilityRuleEntity;
import com.asg.aiusecase.entity.InventoryEntity;
import com.asg.aiusecase.entity.UnitEntity;
import com.asg.aiusecase.repository.CompatibilityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DtoMapper {

    private final CompatibilityRuleRepository compatibilityRuleRepository;

    public InventoryDto toInventoryDto(InventoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new InventoryDto(
                entity.getId(),
                entity.getProductCode(),
                entity.getStockCode(),
                entity.getProductName(),
                entity.getDescription(),
                entity.getMetadataJson(),
                entity.getSerializedJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UnitDto toUnitDto(UnitEntity entity) {
        if (entity == null) {
            return null;
        }
        CompatibilityRuleEntity rule = compatibilityRuleRepository
                .findFirstByInventoryIdAndUnitId(entity.getInventory().getId(), entity.getId())
                .orElse(null);
        return new UnitDto(
                entity.getId(),
                entity.getInventory().getId(),
                entity.getUnitCode(),
                entity.getUnitName(),
                entity.getDescription(),
                entity.getMetadataJson(),
                entity.getSerializedJson(),
                rule == null ? null : rule.isValid(),
                rule == null ? null : rule.getPriority(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MatchedStockDto toMatchedStockDto(InventoryEntity inventory) {
        if (inventory == null) {
            return null;
        }
        return new MatchedStockDto(
                inventory.getId(),
                inventory.getStockCode(),
                inventory.getProductCode(),
                inventory.getProductName()
        );
    }

    public CandidateDto toCandidateDto(ResolvedCandidate candidate) {
        return new CandidateDto(
                candidate.inventory().getId(),
                candidate.unit().getId(),
                candidate.inventory().getStockCode(),
                candidate.inventory().getProductName(),
                candidate.unit().getUnitCode(),
                candidate.unit().getUnitName(),
                round(candidate.confidence()),
                round(candidate.inventorySimilarity()),
                round(candidate.unitSimilarity()),
                candidate.compatible(),
                candidate.reason()
        );
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
