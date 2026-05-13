package com.asg.aiusecase.controller;

import com.asg.aiusecase.cache.CacheService;
import com.asg.aiusecase.dto.InventoryDto;
import com.asg.aiusecase.dto.PublishEmbeddingEventRequest;
import com.asg.aiusecase.dto.UnitDto;
import com.asg.aiusecase.embedding.EmbeddingPipelineService;
import com.asg.aiusecase.messaging.InventoryEvent;
import com.asg.aiusecase.messaging.InventoryEventPublisher;
import com.asg.aiusecase.repository.InventoryRepository;
import com.asg.aiusecase.repository.UnitRepository;
import com.asg.aiusecase.service.DtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryEventPublisher eventPublisher;
    private final InventoryRepository inventoryRepository;
    private final UnitRepository unitRepository;
    private final EmbeddingPipelineService embeddingPipelineService;
    private final CacheService cacheService;
    private final DtoMapper mapper;

    @PostMapping("/stock/embedding-events")
    public ResponseEntity<Map<String, Object>> publishEmbeddingEvent(@Valid @RequestBody PublishEmbeddingEventRequest request) {
        InventoryEvent event = new InventoryEvent(request.type(), request.stockPoid(), request.stockUnitPoid());
        eventPublisher.publish(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "eventId", event.eventId(),
                "status", "PUBLISHED"
        ));
    }

    @DeleteMapping("/stock/embeddings/local")
    public ResponseEntity<Map<String, Object>> clearLocalInventoryEmbeddings() {
        int clearedRows = embeddingPipelineService.clearLocalInventoryEmbeddings();
        cacheService.invalidateSemanticState();
        return ResponseEntity.ok(Map.of(
                "status", "CLEARED",
                "clearedRows", clearedRows
        ));
    }

    @GetMapping("/stock/common-db")
    public ResponseEntity<List<InventoryDto>> getAllStockFromCommonDb() {
        List<InventoryDto> stocks = inventoryRepository.findAllActiveNotDeleted()
                .stream()
                .map(mapper::toInventoryDto)
                .toList();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/stock-unit/common-db")
    public ResponseEntity<List<UnitDto>> getAllStockUnitsFromCommonDb() {
        List<UnitDto> units = unitRepository.findAllActiveNotDeleted()
                .stream()
                .map(mapper::toUnitDto)
                .toList();
        return ResponseEntity.ok(units);
    }

    @PostMapping("/stock/embeddings/sync-all")
    public ResponseEntity<Map<String, Object>> syncAllFromCommonDbToEmbeddings() {
        int stocksSynced = embeddingPipelineService.syncAllStockEmbeddingsFromCommonDb();
        int unitsSynced = embeddingPipelineService.syncAllStockUnitEmbeddingsFromCommonDb();
        cacheService.invalidateSemanticState();
        return ResponseEntity.ok(Map.of(
                "status", "SYNCED",
                "stocksSynced", stocksSynced,
                "unitsSynced", unitsSynced,
                "totalSynced", stocksSynced + unitsSynced
        ));
    }
}
