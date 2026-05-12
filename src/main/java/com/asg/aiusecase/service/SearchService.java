package com.asg.aiusecase.service;

import com.asg.aiusecase.businesslogic.AmbiguityDetector;
import com.asg.aiusecase.businesslogic.BusinessRuleEngine;
import com.asg.aiusecase.businesslogic.FallbackSelector;
import com.asg.aiusecase.businesslogic.ResolvedCandidate;
import com.asg.aiusecase.cache.CacheService;
import com.asg.aiusecase.cache.HashingService;
import com.asg.aiusecase.config.AppProperties;
import com.asg.aiusecase.dto.AmbiguityStatus;
import com.asg.aiusecase.dto.CandidateDto;
import com.asg.aiusecase.dto.ReasoningSource;
import com.asg.aiusecase.dto.SearchRequest;
import com.asg.aiusecase.dto.SearchResponse;
import com.asg.aiusecase.dto.VectorSearchSnapshot;
import com.asg.aiusecase.embedding.EmbeddingService;
import com.asg.aiusecase.metrics.BusinessMetrics;
import com.asg.aiusecase.rag.LlmResolutionService;
import com.asg.aiusecase.vector.InventoryVectorMatch;
import com.asg.aiusecase.vector.UnitVectorMatch;
import com.asg.aiusecase.vector.VectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final AppProperties properties;
    private final HashingService hashingService;
    private final CacheService cacheService;
    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AmbiguityDetector ambiguityDetector;
    private final FallbackSelector fallbackSelector;
    private final LlmResolutionService llmResolutionService;
    private final DtoMapper mapper;
    private final ObjectProvider<BusinessMetrics> businessMetrics;

    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request) {
        String normalized = hashingService.normalize(request.query());
        int topK = request.topK() == null ? properties.getMatching().getTopK() : request.topK();
        double threshold = request.minSimilarity() == null
                ? properties.getMatching().getSimilarityThreshold()
                : request.minSimilarity();
        String modelProfile = String.join("|",
                nullToDefault(request.embeddingModel(), properties.getAi().getEmbeddingModel()),
                nullToDefault(request.chatModel(), properties.getAi().getChatModel()),
                String.valueOf(topK),
                String.valueOf(threshold));
        String exactHash = hashingService.sha256(normalized + "|" + modelProfile);

        Optional<SearchResponse> finalCached = cacheService.get(cacheService.finalKey(exactHash), SearchResponse.class);
        if (finalCached.isPresent()) {
            return withCacheSource(finalCached.get(), "final");
        }

        Optional<SearchResponse> semanticCached = cacheService.get(cacheService.semanticKey(exactHash), SearchResponse.class);
        if (semanticCached.isPresent()) {
            return withCacheSource(semanticCached.get(), "semantic");
        }

        float[] queryEmbedding = embeddingService.embed(request.query(), request.embeddingModel());
        String vectorHash = hashingService.sha256(modelProfile + "|" + java.util.Arrays.toString(queryEmbedding));
        VectorSearchSnapshot snapshot = cacheService.get(cacheService.vectorKey(vectorHash), VectorSearchSnapshot.class)
                .orElseGet(() -> runVectorSearch(queryEmbedding, topK, threshold, vectorHash));

        List<ResolvedCandidate> candidates = businessRuleEngine.evaluate(
                request.query(),
                snapshot.inventories(),
                snapshot.units()
        );
        if (candidates.isEmpty()) {
            SearchResponse response = unmatchedResponse(request, Map.of(
                    "vectorInventoryCandidates", snapshot.inventories().size(),
                    "vectorUnitCandidates", snapshot.units().size()
            ));
            cacheSearch(exactHash, response);
            return response;
        }

        boolean ambiguous = ambiguityDetector.isAmbiguous(candidates);
        if (ambiguous) {
            businessMetrics.ifAvailable(BusinessMetrics::ambiguityDetected);
        }
        boolean allowLlm = request.allowLlm() == null
                ? properties.getMatching().isLlmEnabled()
                : request.allowLlm();

        Optional<ResolvedCandidate> selected = Optional.empty();
        ReasoningSource source = ReasoningSource.VECTOR;
        AmbiguityStatus ambiguityStatus = ambiguous ? AmbiguityStatus.AMBIGUOUS : AmbiguityStatus.NONE;

        if (ambiguous && allowLlm) {
            selected = llmResolutionService.resolve(request.query(), candidates, request.chatModel(), request.fallbackModel());
            if (selected.isPresent()) {
                source = ReasoningSource.LLM;
                ambiguityStatus = AmbiguityStatus.RESOLVED_BY_LLM;
                businessMetrics.ifAvailable(BusinessMetrics::llmResolved);
            }
        }
        if (selected.isEmpty()) {
            selected = fallbackSelector.selectBestBusinessValidCandidate(candidates);
            if (ambiguous) {
                ambiguityStatus = AmbiguityStatus.UNRESOLVED;
            }
        }

        SearchResponse response;
        if (selected.isPresent()) {
            response = toResponse(selected.get(), candidates, source, ambiguityStatus,
                    request.includeCandidates(), metadata(snapshot, ambiguous, allowLlm));
        } else {
            response = unmatchedResponse(request, metadata(snapshot, ambiguous, allowLlm));
        }
        cacheSearch(exactHash, response);
        return response;
    }

    private VectorSearchSnapshot runVectorSearch(float[] queryEmbedding, int topK, double threshold, String vectorHash) {
        CompletableFuture<List<InventoryVectorMatch>> inventoryFuture = CompletableFuture.supplyAsync(
                () -> vectorRepository.searchInventory(queryEmbedding, topK, threshold));
        CompletableFuture<List<UnitVectorMatch>> unitFuture = CompletableFuture.supplyAsync(
                () -> vectorRepository.searchUnits(queryEmbedding, topK, threshold));
        VectorSearchSnapshot snapshot = new VectorSearchSnapshot(inventoryFuture.join(), unitFuture.join());
        cacheService.put(cacheService.vectorKey(vectorHash), snapshot, properties.getCache().getVectorTtl());
        return snapshot;
    }

    private SearchResponse toResponse(ResolvedCandidate selected,
                                      List<ResolvedCandidate> candidates,
                                      ReasoningSource source,
                                      AmbiguityStatus ambiguityStatus,
                                      Boolean includeCandidates,
                                      Map<String, Object> metadata) {
        List<CandidateDto> candidateDtos = Boolean.TRUE.equals(includeCandidates)
                ? candidates.stream()
                .sorted(Comparator.comparingDouble(ResolvedCandidate::confidence).reversed())
                .map(mapper::toCandidateDto)
                .toList()
                : List.of();
        return new SearchResponse(
                mapper.toInventoryDto(selected.inventory()),
                mapper.toMatchedStockDto(selected.inventory()),
                mapper.toUnitDto(selected.unit()),
                round(selected.confidence()),
                round(selected.inventorySimilarity()),
                source,
                metadata,
                ambiguityStatus,
                candidateDtos
        );
    }

    private SearchResponse unmatchedResponse(SearchRequest request, Map<String, Object> metadata) {
        return new SearchResponse(null, null, null, 0.0, 0.0, ReasoningSource.VECTOR,
                metadata, AmbiguityStatus.UNRESOLVED, List.of());
    }

    private Map<String, Object> metadata(VectorSearchSnapshot snapshot, boolean ambiguous, boolean allowLlm) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("vectorInventoryCandidates", snapshot.inventories().size());
        metadata.put("vectorUnitCandidates", snapshot.units().size());
        metadata.put("ambiguous", ambiguous);
        metadata.put("llmAllowed", allowLlm);
        return metadata;
    }

    private void cacheSearch(String exactHash, SearchResponse response) {
        cacheService.put(cacheService.finalKey(exactHash), response, properties.getCache().getFinalTtl());
        cacheService.put(cacheService.semanticKey(exactHash), response, properties.getCache().getSemanticTtl());
    }

    private SearchResponse withCacheSource(SearchResponse cached, String cacheLayer) {
        Map<String, Object> metadata = new LinkedHashMap<>(cached.reasoningMetadata() == null ? Map.of() : cached.reasoningMetadata());
        metadata.put("cacheLayer", cacheLayer);
        return new SearchResponse(
                cached.matchedInventory(),
                cached.matchedStock(),
                cached.matchedUnit(),
                cached.confidenceScore(),
                cached.cosineSimilarity(),
                ReasoningSource.CACHE,
                metadata,
                cached.ambiguityStatus(),
                cached.candidates()
        );
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
