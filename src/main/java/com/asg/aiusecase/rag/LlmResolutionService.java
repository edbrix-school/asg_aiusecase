package com.asg.aiusecase.rag;

import com.asg.aiusecase.businesslogic.ResolvedCandidate;
import com.asg.aiusecase.cache.CacheService;
import com.asg.aiusecase.cache.HashingService;
import com.asg.aiusecase.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmResolutionService {

    private final RagPromptBuilder promptBuilder;
    private final OpenAiResponsesClient openAiResponsesClient;
    private final CacheService cacheService;
    private final HashingService hashingService;
    private final AppProperties properties;

    public LlmClientResult parseQuantityBatch(String systemPrompt, String userPrompt, String modelOverride) {
        return openAiResponsesClient.parseQuantityBatch(systemPrompt, userPrompt, modelOverride);
    }

    public Optional<ResolvedCandidate> resolve(String query,
                                               List<ResolvedCandidate> candidates,
                                               String modelOverride,
                                               String fallbackModelOverride) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        List<ResolvedCandidate> allowed = candidates.stream()
                .filter(ResolvedCandidate::compatible)
                .sorted(Comparator.comparingDouble(ResolvedCandidate::confidence).reversed())
                .limit(properties.getMatching().getMaxLlmCandidates())
                .toList();
        if (allowed.isEmpty()) {
            return Optional.empty();
        }

        String selectedModel = modelOverride == null || modelOverride.isBlank()
                ? properties.getAi().getChatModel()
                : modelOverride;
        String hash = hashingService.sha256(query + "|" + selectedModel + "|" + allowed.stream()
                .map(c -> c.inventory().getId() + ":" + c.unit().getId() + ":" + c.confidence())
                .toList());
        String cacheKey = cacheService.llmKey(hash);
        Optional<LlmResolutionResult> cached = cacheService.get(cacheKey, LlmResolutionResult.class);
        if (cached.isPresent()) {
            return validate(cached.get(), allowed);
        }

        Optional<LlmResolutionResult> primary = callModel(query, allowed, selectedModel);
        Optional<ResolvedCandidate> validatedPrimary = primary.flatMap(result -> validate(result, allowed));
        if (validatedPrimary.isPresent()) {
            cacheService.put(cacheKey, primary.get(), properties.getCache().getLlmTtl());
            return validatedPrimary;
        }

        String fallbackModel = fallbackModelOverride == null || fallbackModelOverride.isBlank()
                ? properties.getAi().getFallbackModel()
                : fallbackModelOverride;
        if (fallbackModel != null && !fallbackModel.isBlank() && !fallbackModel.equals(selectedModel)) {
            Optional<LlmResolutionResult> fallback = callModel(query, allowed, fallbackModel);
            Optional<ResolvedCandidate> validatedFallback = fallback.flatMap(result -> validate(result, allowed));
            validatedFallback.ifPresent(candidate -> cacheService.put(cacheKey, fallback.get(), properties.getCache().getLlmTtl()));
            return validatedFallback;
        }
        return Optional.empty();
    }

    private Optional<LlmResolutionResult> callModel(String query, List<ResolvedCandidate> allowed, String model) {
        try {
            LlmClientResult result = openAiResponsesClient.resolve(
                    promptBuilder.systemPrompt(),
                    promptBuilder.userPrompt(query, allowed),
                    model
            );
            JsonNode payload = result.payload();
            if (payload.path("stockPoid").isNull() || payload.path("stockUnitPoid").isNull()) {
                return Optional.empty();
            }
            return Optional.of(new LlmResolutionResult(
                    payload.path("stockPoid").asLong(),
                    payload.path("stockUnitPoid").asLong(),
                    payload.path("confidence").asDouble(0.0),
                    payload.path("reason").asText("")
            ));
        } catch (Exception e) {
            log.warn("LLM ambiguity resolution failed for model={}: {}", model, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ResolvedCandidate> validate(LlmResolutionResult result, List<ResolvedCandidate> allowed) {
        return allowed.stream()
                .filter(candidate -> candidate.inventory().getId().equals(result.stockPoid()))
                .filter(candidate -> candidate.unit().getId().equals(result.stockUnitPoid()))
                .findFirst();
    }
}
