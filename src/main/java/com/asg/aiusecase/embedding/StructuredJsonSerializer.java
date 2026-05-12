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

    public Map<String, Object> inventoryPayload(InventoryEntity inventory) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productCode", inventory.getProductCode());
        payload.put("stockCode", inventory.getStockCode());
        payload.put("productName", inventory.getProductName());
        payload.put("description", inventory.getDescription());
        return payload;
    }

    public Map<String, Object> unitPayload(UnitEntity unit) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("unitCode", unit.getUnitCode());
        payload.put("unitName", unit.getUnitName());
        payload.put("description", unit.getDescription());
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
