package com.asg.aiusecase.rag;

import com.asg.aiusecase.businesslogic.ResolvedCandidate;
import com.asg.aiusecase.businesslogic.CompatibilityValidator;
import com.asg.aiusecase.entity.CompatibilityRuleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RagPromptBuilder {

    private final CompatibilityValidator compatibilityValidator;

    public String systemPrompt() {
        return """
                You are an inventory ambiguity resolver.
                You may only choose from the provided candidate inventoryId/unitId pairs.
                You must not invent products, stock codes, units, or compatibility rules.
                Business compatibility rules are hard constraints and override semantic preference.
                Return strict JSON only with this shape:
                {"inventoryId":number|null,"unitId":number|null,"confidence":number,"reason":"short reason"}
                If no candidate is sufficiently supported, return null inventoryId and null unitId.
                """;
    }

    public String userPrompt(String query, List<ResolvedCandidate> candidates) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Original query:\n").append(query).append("\n\n");
        prompt.append("Allowed candidates:\n");
        prompt.append("inventoryId|stockCode|productName|unitId|unitCode|unitName|inventorySimilarity|unitSimilarity|confidence|reason\n");
        for (ResolvedCandidate candidate : candidates) {
            prompt.append(candidate.inventory().getId()).append("|")
                    .append(candidate.inventory().getStockCode()).append("|")
                    .append(candidate.inventory().getProductName()).append("|")
                    .append(candidate.unit().getId()).append("|")
                    .append(candidate.unit().getUnitCode()).append("|")
                    .append(candidate.unit().getUnitName()).append("|")
                    .append(round(candidate.inventorySimilarity())).append("|")
                    .append(round(candidate.unitSimilarity())).append("|")
                    .append(round(candidate.confidence())).append("|")
                    .append(candidate.reason()).append("\n");
        }
        prompt.append("\nAllowed compatibility mappings:\n");
        prompt.append("inventoryId|unitId|priority\n");
        candidates.stream()
                .map(candidate -> candidate.inventory().getId())
                .distinct()
                .forEach(inventoryId -> {
                    for (CompatibilityRuleEntity rule : compatibilityValidator.validRules(inventoryId)) {
                        prompt.append(rule.getInventory().getId()).append("|")
                                .append(rule.getUnit().getId()).append("|")
                                .append(rule.getPriority()).append("\n");
                    }
                });
        return prompt.toString();
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
