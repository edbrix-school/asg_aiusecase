package com.asg.aiusecase.embedding;

import com.asg.aiusecase.entity.InventoryEntity;
import com.asg.aiusecase.entity.UnitEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StructuredJsonSerializer {

    private final ObjectMapper objectMapper;

    public Map<String, Object> inventoryPayload(InventoryEntity stock) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stockPoid", stock.getStockPoid());
        payload.put("stockCode", stock.getStockCode());
        payload.put("stockName", stock.getStockName());
        payload.put("stockName2", stock.getStockName2());
        payload.put("stockDescription", stock.getStockDescription());
        payload.put("active", stock.getActive());
        payload.put("deleted", stock.getDeleted());
        return payload;
    }

    public Map<String, Object> unitPayload(UnitEntity stockUnit) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stockUnitPoid", stockUnit.getStockUnitPoid());
        payload.put("stockUnitCode", stockUnit.getStockUnitCode());
        payload.put("stockUnitName", stockUnit.getStockUnitName());
        payload.put("active", stockUnit.getActive());
        payload.put("deleted", stockUnit.getDeleted());
        return payload;
    }

    public String compactString(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize compact JSON payload", e);
        }
    }
}
