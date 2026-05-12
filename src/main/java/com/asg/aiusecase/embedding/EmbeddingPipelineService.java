package com.asg.aiusecase.embedding;

import com.asg.aiusecase.entity.InventoryEntity;
import com.asg.aiusecase.entity.UnitEntity;
import com.asg.aiusecase.repository.InventoryRepository;
import com.asg.aiusecase.repository.UnitRepository;
import com.asg.aiusecase.vector.VectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingPipelineService {

    private final InventoryRepository inventoryRepository;
    private final UnitRepository unitRepository;
    private final StructuredJsonSerializer serializer;
    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;

    @Transactional
    public void regenerateInventoryEmbedding(Long inventoryId) {
        inventoryRepository.findById(inventoryId).ifPresentOrElse(this::regenerateInventoryEmbedding,
                () -> log.warn("Skipping inventory embedding regeneration; id={} not found", inventoryId));
    }

    @Transactional
    public void regenerateUnitEmbedding(Long unitId) {
        unitRepository.findById(unitId).ifPresentOrElse(this::regenerateUnitEmbedding,
                () -> log.warn("Skipping unit embedding regeneration; id={} not found", unitId));
    }

    public void regenerateInventoryEmbedding(InventoryEntity inventory) {
        Map<String, Object> payload = serializer.inventoryPayload(inventory);
        float[] embedding = embeddingService.embed(serializer.compactString(payload), null);
        vectorRepository.updateInventoryEmbedding(inventory.getId(), payload, embedding);
        log.info("Regenerated inventory embedding id={} stockCode={}", inventory.getId(), inventory.getStockCode());
    }

    public void regenerateUnitEmbedding(UnitEntity unit) {
        Map<String, Object> payload = serializer.unitPayload(unit);
        float[] embedding = embeddingService.embed(serializer.compactString(payload), null);
        vectorRepository.updateUnitEmbedding(unit.getId(), payload, embedding);
        log.info("Regenerated unit embedding id={} unitCode={}", unit.getId(), unit.getUnitCode());
    }
}
