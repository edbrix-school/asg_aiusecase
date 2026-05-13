package com.asg.aiusecase.orchestration;

import com.asg.aiusecase.businesslogic.ResolvedCandidate;
import com.asg.aiusecase.config.AppProperties;
import com.asg.aiusecase.dto.SearchRequest;
import com.asg.aiusecase.dto.SearchResponse;
import com.asg.aiusecase.ingestion.InventoryLine;
import com.asg.aiusecase.ingestion.SearchOptions;
import com.asg.aiusecase.ingestion.file.FileIngestionService;
import com.asg.aiusecase.ingestion.parser.ParsedQuery;
import com.asg.aiusecase.ingestion.parser.QueryPayloadParser;
import com.asg.aiusecase.orchestration.batch.BatchAmbiguityItem;
import com.asg.aiusecase.orchestration.batch.BatchLlmService;
import com.asg.aiusecase.service.LlmSelectionOverride;
import com.asg.aiusecase.service.ParsedQuantity;
import com.asg.aiusecase.service.SearchPreparation;
import com.asg.aiusecase.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventorySearchOrchestrator {

    private final QueryPayloadParser queryPayloadParser;
    private final FileIngestionService fileIngestionService;
    private final BatchLlmService batchLlmService;
    private final SearchService searchService;
    private final AppProperties properties;

    public List<SearchResponse> search(String queryPayload, String metaPayload, MultipartFile file) {
        if ((queryPayload == null || queryPayload.isBlank()) && (file == null || file.isEmpty())) {
            throw new IllegalArgumentException("Either query or file must be provided");
        }

        ParsedQuery parsedQuery = queryPayloadParser.parse(queryPayload);
        SearchOptions metaDefaults = queryPayloadParser.parseOptionsPayload(metaPayload);
        List<InventoryLine> lines = new ArrayList<>();
        lines.addAll(parsedQuery.lines());
        lines.addAll(fileIngestionService.ingest(file, "file"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("No inventory lines found");
        }

        SearchOptions defaults = parsedQuery.defaults().merge(metaDefaults);
        List<InventoryLine> merged = lines.stream()
                .map(line -> new InventoryLine(
                        line.id(),
                        line.rawText(),
                        line.normalizedText(),
                        line.source(),
                        line.options().merge(defaults)
                ))
                .toList();

        Map<String, ParsedQuantity> quantities = batchLlmService.parseQuantities(merged);

        List<InventoryLine> ordered = new ArrayList<>(merged);
        Map<String, SearchPreparation> preparations = new HashMap<>();
        for (InventoryLine line : ordered) {
            SearchRequest request = toSearchRequest(line);
            ParsedQuantity quantity = quantities.get(line.id());
            SearchPreparation preparation = searchService.prepare(request, quantity);
            preparations.put(line.id(), preparation);
        }

        List<BatchAmbiguityItem> ambiguityItems = new ArrayList<>();
        for (InventoryLine line : ordered) {
            SearchPreparation preparation = preparations.get(line.id());
            if (preparation == null || preparation.cachedResponse() != null) {
                continue;
            }
            if (!preparation.ambiguous() || !preparation.allowLlm()) {
                continue;
            }
            List<ResolvedCandidate> allowed = preparation.candidates().stream()
                    .filter(ResolvedCandidate::compatible)
                    .sorted(Comparator.comparingDouble(ResolvedCandidate::confidence).reversed())
                    .limit(properties.getMatching().getMaxLlmCandidates())
                    .toList();
            if (allowed.isEmpty()) {
                continue;
            }
            SearchRequest request = preparation.request();
            ambiguityItems.add(new BatchAmbiguityItem(
                    line.id(),
                    request.query(),
                    allowed,
                    request.chatModel(),
                    request.fallbackModel()
            ));
        }

        Map<String, LlmSelectionOverride> overrides = batchLlmService.resolveAmbiguities(ambiguityItems);

        List<SearchResponse> responses = new ArrayList<>();
        for (InventoryLine line : ordered) {
            SearchPreparation preparation = preparations.get(line.id());
            LlmSelectionOverride override = overrides.get(line.id());
            responses.add(searchService.finalizeResponse(preparation, override));
        }
        return responses;
    }

    private SearchRequest toSearchRequest(InventoryLine line) {
        SearchOptions options = line.options();
        return new SearchRequest(
                line.normalizedText(),
                options.topK(),
                options.minSimilarity(),
                options.embeddingModel(),
                options.chatModel(),
                options.fallbackModel(),
                options.allowLlm(),
                options.includeCandidates(),
                options.llmParseQuantity(),
                options.llmParseModel()
        );
    }
}
