package com.asg.aiusecase.service;

import com.asg.aiusecase.cache.CacheService;
import com.asg.aiusecase.dto.CreateInventoryRequest;
import com.asg.aiusecase.dto.InventoryDto;
import com.asg.aiusecase.dto.UpdateInventoryRequest;
import com.asg.aiusecase.embedding.StructuredJsonSerializer;
import com.asg.aiusecase.entity.InventoryEntity;
import com.asg.aiusecase.exception.ConflictException;
import com.asg.aiusecase.exception.NotFoundException;
import com.asg.aiusecase.messaging.InventoryEvent;
import com.asg.aiusecase.messaging.InventoryEventPublisher;
import com.asg.aiusecase.messaging.InventoryEventType;
import com.asg.aiusecase.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final DtoMapper mapper;
    private final StructuredJsonSerializer serializer;
    private final InventoryEventPublisher eventPublisher;
    private final CacheService cacheService;

    @Transactional
    public InventoryDto create(CreateInventoryRequest request) {
        if (inventoryRepository.existsByProductCodeIgnoreCase(request.productCode())) {
            throw new ConflictException("Inventory productCode already exists: " + request.productCode());
        }
        if (inventoryRepository.existsByStockCodeIgnoreCase(request.stockCode())) {
            throw new ConflictException("Inventory stockCode already exists: " + request.stockCode());
        }
        InventoryEntity entity = new InventoryEntity();
        entity.setProductCode(request.productCode().trim());
        entity.setStockCode(request.stockCode().trim());
        entity.setProductName(request.productName().trim());
        entity.setDescription(trimToNull(request.description()));
        entity.setMetadataJson(request.metadata() == null ? Map.of() : request.metadata());
        entity.setSerializedJson(serializer.inventoryPayload(entity));
        InventoryEntity saved = inventoryRepository.save(entity);
        eventPublisher.publish(new InventoryEvent(InventoryEventType.PRODUCT_CREATED, saved.getId(), null));
        cacheService.invalidateSemanticState();
        return mapper.toInventoryDto(saved);
    }

    @Transactional
    public InventoryDto update(Long id, UpdateInventoryRequest request) {
        InventoryEntity entity = inventoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + id));
        if (StringUtils.hasText(request.productCode())) {
            entity.setProductCode(request.productCode().trim());
        }
        if (StringUtils.hasText(request.stockCode())) {
            entity.setStockCode(request.stockCode().trim());
        }
        if (StringUtils.hasText(request.productName())) {
            entity.setProductName(request.productName().trim());
        }
        if (request.description() != null) {
            entity.setDescription(trimToNull(request.description()));
        }
        if (request.metadata() != null) {
            entity.setMetadataJson(request.metadata());
        }
        entity.setSerializedJson(serializer.inventoryPayload(entity));
        InventoryEntity saved = inventoryRepository.save(entity);
        eventPublisher.publish(new InventoryEvent(InventoryEventType.PRODUCT_UPDATED, saved.getId(), null));
        cacheService.invalidateSemanticState();
        return mapper.toInventoryDto(saved);
    }

    @Transactional(readOnly = true)
    public InventoryDto get(Long id) {
        return inventoryRepository.findById(id).map(mapper::toInventoryDto)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> list() {
        return inventoryRepository.findAll().stream().map(mapper::toInventoryDto).toList();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
