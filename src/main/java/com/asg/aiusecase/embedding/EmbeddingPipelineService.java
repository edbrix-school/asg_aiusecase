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
    public void regenerateStockEmbedding(Long stockPoid) {
        inventoryRepository.findById(stockPoid).ifPresentOrElse(this::regenerateStockEmbedding,
                () -> log.warn("Skipping stock embedding regeneration; STOCK_POID={} not found", stockPoid));
    }

    @Transactional
    public void regenerateStockUnitEmbedding(Long stockUnitPoid) {
        unitRepository.findById(stockUnitPoid).ifPresentOrElse(this::regenerateStockUnitEmbedding,
                () -> log.warn("Skipping stock unit embedding regeneration; STOCK_UNIT_POID={} not found", stockUnitPoid));
    }

    public void regenerateStockEmbedding(InventoryEntity stock) {
        Map<String, Object> payload = serializer.inventoryPayload(stock);
        float[] embedding = embeddingService.embed(serializer.compactString(payload), null);
        vectorRepository.upsertStockEmbedding(stock, payload, embedding);
        log.info("Regenerated stock embedding STOCK_POID={} STOCK_CODE={}", stock.getId(), stock.getStockCode());
    }

    public void regenerateStockUnitEmbedding(UnitEntity stockUnit) {
        Map<String, Object> payload = serializer.unitPayload(stockUnit);
        float[] embedding = embeddingService.embed(serializer.compactString(payload), null);
        vectorRepository.upsertStockUnitEmbedding(stockUnit, payload, embedding);
        log.info("Regenerated stock unit embedding STOCK_UNIT_POID={} STOCK_UNIT_CODE={}", stockUnit.getId(), stockUnit.getUnitCode());
    }

    public void deleteStockEmbedding(Long stockPoid) {
        vectorRepository.deleteStockEmbedding(stockPoid);
        log.info("Deleted stock embedding STOCK_POID={}", stockPoid);
    }

    public void deleteStockUnitEmbedding(Long stockUnitPoid) {
        vectorRepository.deleteStockUnitEmbedding(stockUnitPoid);
        log.info("Deleted stock unit embedding STOCK_UNIT_POID={}", stockUnitPoid);
    }
}
