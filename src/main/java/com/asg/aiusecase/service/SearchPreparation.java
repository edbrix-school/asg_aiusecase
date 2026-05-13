package com.asg.aiusecase.service;

import com.asg.aiusecase.businesslogic.ResolvedCandidate;
import com.asg.aiusecase.dto.SearchRequest;
import com.asg.aiusecase.dto.SearchResponse;

import java.util.List;
import java.util.Map;

public record SearchPreparation(
        SearchRequest request,
        String exactHash,
        List<ResolvedCandidate> candidates,
        Map<String, Object> metadata,
        boolean ambiguous,
        boolean allowLlm,
        Double parsedQuantity,
        String parsedUnit,
        String parsedBatch,
        SearchResponse cachedResponse
) {
    public static SearchPreparation cached(SearchResponse response) {
        return new SearchPreparation(null, null, List.of(), Map.of(), false, false,
                null, null, null, response);
    }
}
