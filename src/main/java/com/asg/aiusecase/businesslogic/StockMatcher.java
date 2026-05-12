package com.asg.aiusecase.businesslogic;

import com.asg.aiusecase.entity.InventoryEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class StockMatcher {

    public boolean exactStockMatch(String query, InventoryEntity inventory) {
        String normalized = normalize(query);
        return containsToken(normalized, inventory.getStockCode())
                || containsToken(normalized, inventory.getProductCode())
                || normalize(inventory.getProductName()).equals(normalized);
    }

    public double exactBoost(String query, InventoryEntity inventory) {
        return exactStockMatch(query, inventory) ? 0.18 : 0.0;
    }

    private boolean containsToken(String normalizedQuery, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return normalizedQuery.contains(normalize(value));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
