package com.asg.aiusecase.service;

import com.asg.aiusecase.cache.CacheService;
import com.asg.aiusecase.dto.CreateUnitRequest;
import com.asg.aiusecase.dto.UnitDto;
import com.asg.aiusecase.dto.UpdateUnitRequest;
import com.asg.aiusecase.embedding.StructuredJsonSerializer;
import com.asg.aiusecase.entity.CompatibilityRuleEntity;
import com.asg.aiusecase.entity.InventoryEntity;
import com.asg.aiusecase.entity.UnitEntity;
import com.asg.aiusecase.exception.ConflictException;
import com.asg.aiusecase.exception.NotFoundException;
import com.asg.aiusecase.messaging.InventoryEvent;
import com.asg.aiusecase.messaging.InventoryEventPublisher;
import com.asg.aiusecase.messaging.InventoryEventType;
import com.asg.aiusecase.repository.CompatibilityRuleRepository;
import com.asg.aiusecase.repository.InventoryRepository;
import com.asg.aiusecase.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final InventoryRepository inventoryRepository;
    private final CompatibilityRuleRepository compatibilityRuleRepository;
    private final StructuredJsonSerializer serializer;
    private final DtoMapper mapper;
    private final InventoryEventPublisher eventPublisher;
    private final CacheService cacheService;

    @Transactional
    public UnitDto create(CreateUnitRequest request) {
        InventoryEntity inventory = inventoryRepository.findById(request.inventoryId())
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + request.inventoryId()));
        unitRepository.findFirstByInventoryIdAndUnitCodeIgnoreCase(inventory.getId(), request.unitCode())
                .ifPresent(existing -> {
                    throw new ConflictException("Unit code already exists for inventory: " + request.unitCode());
                });
        UnitEntity unit = new UnitEntity();
        unit.setInventory(inventory);
        unit.setUnitCode(request.unitCode().trim());
        unit.setUnitName(request.unitName().trim());
        unit.setDescription(trimToNull(request.description()));
        unit.setMetadataJson(request.metadata() == null ? Map.of() : request.metadata());
        unit.setSerializedJson(serializer.unitPayload(unit));
        UnitEntity saved = unitRepository.save(unit);
        upsertCompatibility(saved, Boolean.TRUE.equals(request.validForInventory()), priorityOrDefault(request.compatibilityPriority()));
        eventPublisher.publish(new InventoryEvent(InventoryEventType.UNIT_CREATED, inventory.getId(), saved.getId()));
        cacheService.invalidateSemanticState();
        return mapper.toUnitDto(saved);
    }

    @Transactional
    public UnitDto update(Long id, UpdateUnitRequest request) {
        UnitEntity unit = unitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unit not found: " + id));
        if (StringUtils.hasText(request.unitCode())) {
            unit.setUnitCode(request.unitCode().trim());
        }
        if (StringUtils.hasText(request.unitName())) {
            unit.setUnitName(request.unitName().trim());
        }
        if (request.description() != null) {
            unit.setDescription(trimToNull(request.description()));
        }
        if (request.metadata() != null) {
            unit.setMetadataJson(request.metadata());
        }
        unit.setSerializedJson(serializer.unitPayload(unit));
        UnitEntity saved = unitRepository.save(unit);
        if (request.validForInventory() != null || request.compatibilityPriority() != null) {
            upsertCompatibility(saved,
                    request.validForInventory() != null && request.validForInventory(),
                    priorityOrDefault(request.compatibilityPriority()));
        }
        eventPublisher.publish(new InventoryEvent(InventoryEventType.UNIT_UPDATED, saved.getInventory().getId(), saved.getId()));
        cacheService.invalidateSemanticState();
        return mapper.toUnitDto(saved);
    }

    @Transactional(readOnly = true)
    public UnitDto get(Long id) {
        return unitRepository.findById(id).map(mapper::toUnitDto)
                .orElseThrow(() -> new NotFoundException("Unit not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<UnitDto> listByInventory(Long inventoryId) {
        return unitRepository.findByInventoryId(inventoryId).stream().map(mapper::toUnitDto).toList();
    }

    private void upsertCompatibility(UnitEntity unit, boolean valid, int priority) {
        CompatibilityRuleEntity rule = compatibilityRuleRepository
                .findFirstByInventoryIdAndUnitId(unit.getInventory().getId(), unit.getId())
                .orElseGet(CompatibilityRuleEntity::new);
        rule.setInventory(unit.getInventory());
        rule.setUnit(unit);
        rule.setValid(valid);
        rule.setPriority(priority);
        compatibilityRuleRepository.save(rule);
    }

    private int priorityOrDefault(Integer priority) {
        return priority == null ? 100 : priority;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
