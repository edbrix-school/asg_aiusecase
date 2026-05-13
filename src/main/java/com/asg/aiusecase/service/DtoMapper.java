package com.asg.aiusecase.service;

import com.asg.aiusecase.businesslogic.ResolvedCandidate;
import com.asg.aiusecase.dto.CandidateDto;
import com.asg.aiusecase.dto.InventoryDto;
import com.asg.aiusecase.dto.MatchedStockDto;
import com.asg.aiusecase.dto.MatchedUnitDto;
import com.asg.aiusecase.dto.UnitDto;
import com.asg.aiusecase.entity.InventoryEntity;
import com.asg.aiusecase.entity.UnitEntity;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public InventoryDto toInventoryDto(InventoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new InventoryDto(
                entity.getId(),
                entity.getStockCode(),
                entity.getStockName(),
                entity.getStockDescription(),
                entity.getMetadataJson(),
                entity.getSerializedJson(),
                null,
                null
        );
    }

    public UnitDto toUnitDto(UnitEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UnitDto(
                entity.getId(),
                entity.getUnitCode(),
                entity.getUnitName(),
                entity.getMetadataJson(),
                entity.getSerializedJson(),
                null,
                null
        );
    }

    public MatchedUnitDto toMatchedUnitDto(UnitEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MatchedUnitDto(
                entity.getId(),
                entity.getUnitCode(),
                entity.getUnitName()
        );
    }

    public MatchedStockDto toMatchedStockDto(InventoryEntity inventory) {
        if (inventory == null) {
            return null;
        }
        return new MatchedStockDto(
                inventory.getId(),
                inventory.getStockCode(),
                inventory.getStockName()
        );
    }

    public CandidateDto toCandidateDto(ResolvedCandidate candidate) {
        return new CandidateDto(
                candidate.inventory().getId(),
                candidate.unit().getId(),
                candidate.inventory().getStockCode(),
                candidate.inventory().getStockName(),
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
