package com.asg.aiusecase.businesslogic;

import com.asg.aiusecase.entity.CompatibilityRuleEntity;
import com.asg.aiusecase.entity.UnitEntity;
import com.asg.aiusecase.repository.CompatibilityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CompatibilityValidator {

    private final CompatibilityRuleRepository compatibilityRuleRepository;

    public boolean isCompatible(Long inventoryId, Long unitId) {
        return compatibilityRuleRepository.findFirstByInventoryIdAndUnitId(inventoryId, unitId)
                .map(CompatibilityRuleEntity::isValid)
                .orElse(false);
    }

    public Optional<UnitEntity> highestPriorityValidUnit(Long inventoryId) {
        return compatibilityRuleRepository.findByInventoryIdAndValidTrueOrderByPriorityAsc(inventoryId).stream()
                .map(CompatibilityRuleEntity::getUnit)
                .findFirst();
    }

    public List<CompatibilityRuleEntity> validRules(Long inventoryId) {
        return compatibilityRuleRepository.findByInventoryIdAndValidTrueOrderByPriorityAsc(inventoryId);
    }
}
