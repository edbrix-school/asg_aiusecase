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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.asg.aiusecase.rag.LlmClientResult;
import com.fasterxml.jackson.databind.JsonNode;

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

    private static final Pattern QTY_UNIT_AT_END = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([A-Za-z]+)?\\s*$");

    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request) {
        String normalized = hashingService.normalize(request.query());
        int topK = request.topK() == null ? properties.getMatching().getTopK() : request.topK();
        double threshold = request.minSimilarity() == null
                ? properties.getMatching().getSimilarityThreshold()
                : request.minSimilarity();
        String parseModel = nullToDefault(request.llmParseModel(), "gpt-4.1-nano");
        boolean parseWithLlm = Boolean.TRUE.equals(request.llmParseQuantity());
        String modelProfile = String.join("|",
                nullToDefault(request.embeddingModel(), properties.getAi().getEmbeddingModel()),
                nullToDefault(request.chatModel(), properties.getAi().getChatModel()),
                String.valueOf(topK),
                String.valueOf(threshold),
                String.valueOf(parseWithLlm),
                parseModel);
        String exactHash = hashingService.sha256(normalized + "|" + modelProfile);

        // Parse quantity/unit from query: regex by default, LLM if enabled
        Double parsedQuantity = null;
        String parsedUnit = null;
        String parsedBatch = null;
        if (parseWithLlm) {
            try {
                String qtySys = "Extract quantity, unit (normalize to short form), and batch identifier from the user query. Respond with JSON {quantity:number|null,unit:string|null,batch:string|null}.";
                String qtyUser = "Query: \"" + request.query() + "\"\nRespond only JSON.";
                LlmClientResult qtyRes = llmResolutionService.parseQuantityBatch(qtySys, qtyUser, parseModel);
                JsonNode qtyPayload = qtyRes.payload();
                if (qtyPayload != null) {
                    parsedQuantity = qtyPayload.path("quantity").isNumber() ? qtyPayload.path("quantity").asDouble() : null;
                    parsedUnit = qtyPayload.path("unit").isTextual() ? qtyPayload.path("unit").asText().toUpperCase() : null;
                    parsedBatch = qtyPayload.path("batch").isTextual() ? qtyPayload.path("batch").asText() : null;
                }
            } catch (Exception e) {
                log.debug("LLM quantity parse failed: {}", e.getMessage());
            }
        } else {
            try {
                String q = request.query() != null ? request.query().trim() : "";
                Matcher m = QTY_UNIT_AT_END.matcher(q);
                if (m.find()) {
                    String qStr = m.group(1);
                    String uStr = m.group(2);
                    if (qStr != null && !qStr.isBlank()) {
                        parsedQuantity = Double.parseDouble(qStr);
                    }
                    if (uStr != null && !uStr.isBlank()) {
                        parsedUnit = uStr.trim().toUpperCase();
                    }
                }
            } catch (Exception e) {
                // ignore parse errors; leave parsedQuantity/parsedUnit null
                log.debug("Regex quantity parse failed: {}", e.getMessage());
            }
        }

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
                snapshot.units(),
                parsedUnit
        );
        if (candidates.isEmpty()) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("vectorInventoryCandidates", snapshot.inventories().size());
            meta.put("vectorUnitCandidates", snapshot.units().size());
            SearchResponse response = unmatchedResponse(request, meta, parsedQuantity, parsedUnit, parsedBatch);
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

        // prepare reasoning metadata map early so cheap parse can add to it
        Map<String, Object> meta = metadata(snapshot, ambiguous, allowLlm);
        if (ambiguous && allowLlm) {
            // Resolve ambiguity using LLM for stock/unit selection
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
                            request.includeCandidates(), meta, parsedQuantity, parsedUnit, parsedBatch);
                } else {
                    response = unmatchedResponse(request, meta, parsedQuantity, parsedUnit, parsedBatch);
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
                                      Map<String, Object> metadata,
                                      Double parsedQuantity,
                                      String parsedUnit,
                                      String parsedBatch) {
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
                candidateDtos,
                parsedQuantity,
                parsedUnit,
                parsedBatch
        );
    }

    private SearchResponse unmatchedResponse(SearchRequest request, Map<String, Object> metadata,
                                             Double parsedQuantity, String parsedUnit, String parsedBatch) {
        return new SearchResponse(null, null, null, 0.0, 0.0, ReasoningSource.VECTOR,
                metadata, AmbiguityStatus.UNRESOLVED, List.of(), parsedQuantity, parsedUnit, parsedBatch);
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
                cached.matchedStockMaster(),
                cached.matchedStock(),
                cached.matchedStockUnit(),
                cached.confidenceScore(),
                cached.cosineSimilarity(),
                ReasoningSource.CACHE,
                metadata,
                cached.ambiguityStatus(),
                cached.candidates(),
                cached.parsedQuantity(),
                cached.parsedUnit(),
                cached.parsedBatch()
        );
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
