package com.asg.aiusecase.rag;

import com.asg.aiusecase.businesslogic.ResolvedCandidate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagPromptBuilder {

    public String systemPrompt() {
        return """
                You are a stock ambiguity resolver.
                You may only choose from the provided candidate stockPoid/stockUnitPoid pairs.
                You must not invent stock codes, stock names, stock units, or IDs.
                Return strict JSON only with this shape:
                {"stockPoid":number|null,"stockUnitPoid":number|null,"confidence":number,"reason":"short reason"}
                If no candidate is sufficiently supported, return null stockPoid and null stockUnitPoid.
                """;
    }

    public String userPrompt(String query, List<ResolvedCandidate> candidates) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Original query:\n").append(query).append("\n\n");
        prompt.append("Allowed candidates:\n");
        prompt.append("stockPoid|stockCode|stockName|stockUnitPoid|stockUnitCode|stockUnitName|stockSimilarity|unitSimilarity|confidence|reason\n");
        for (ResolvedCandidate candidate : candidates) {
            prompt.append(candidate.inventory().getId()).append("|")
                    .append(candidate.inventory().getStockCode()).append("|")
                    .append(candidate.inventory().getStockName()).append("|")
                    .append(candidate.unit().getId()).append("|")
                    .append(candidate.unit().getUnitCode()).append("|")
                    .append(candidate.unit().getUnitName()).append("|")
                    .append(round(candidate.inventorySimilarity())).append("|")
                    .append(round(candidate.unitSimilarity())).append("|")
                    .append(round(candidate.confidence())).append("|")
                    .append(candidate.reason()).append("\n");
        }
        return prompt.toString();
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
