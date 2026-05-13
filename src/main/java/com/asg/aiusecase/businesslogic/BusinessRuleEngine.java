package com.asg.aiusecase.businesslogic;

import com.asg.aiusecase.entity.InventoryEntity;
import com.asg.aiusecase.entity.SynonymEntity;
import com.asg.aiusecase.entity.UnitEntity;
import com.asg.aiusecase.repository.InventoryRepository;
import com.asg.aiusecase.repository.UnitRepository;
import com.asg.aiusecase.vector.InventoryVectorMatch;
import com.asg.aiusecase.vector.UnitVectorMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusinessRuleEngine {

    private final InventoryRepository inventoryRepository;
    private final UnitRepository unitRepository;
    private final SynonymResolver synonymResolver;
    private final StockMatcher stockMatcher;
    private final UnitMatcher unitMatcher;
    private final ConfidenceCalculator confidenceCalculator;

    @Transactional(readOnly = true)
    public List<ResolvedCandidate> evaluate(String query,
                                            List<InventoryVectorMatch> inventoryMatches,
                                            List<UnitVectorMatch> unitMatches) {
        Optional<SynonymEntity> inventorySynonym = synonymResolver.findInventorySynonym(query);
        Optional<SynonymEntity> unitSynonym = synonymResolver.findUnitSynonym(query);

        List<InventoryEntity> exactInventoryMatches = exactInventoryMatches(query, inventorySynonym);
        List<UnitEntity> exactUnitMatches = exactUnitMatches(query, unitSynonym);

        List<ResolvedCandidate> vectorCandidates = inventoryMatches.stream()
                .flatMap(match -> inventoryRepository.findById(match.id()).stream()
                        .flatMap(inventory -> resolveUnits(query, inventory, match.cosineSimilarity(), exactUnitMatches, unitMatches, unitSynonym).stream()))
                .toList();

        List<ResolvedCandidate> exactCandidates = exactInventoryMatches.stream()
                .flatMap(inventory -> resolveUnits(query, inventory, 1.0, exactUnitMatches, unitMatches, unitSynonym).stream())
                .toList();

        return java.util.stream.Stream.concat(exactCandidates.stream(), vectorCandidates.stream())
                .collect(java.util.stream.Collectors.toMap(
                        c -> c.inventory().getId() + ":" + c.unit().getId(),
                        c -> c,
                        (left, right) -> left.confidence() >= right.confidence() ? left : right
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(ResolvedCandidate::confidence).reversed())
                .toList();
    }

    private List<InventoryEntity> exactInventoryMatches(String query, Optional<SynonymEntity> inventorySynonym) {
        LinkedHashSet<InventoryEntity> matches = new LinkedHashSet<>();
        String normalized = query == null ? "" : query.trim();
        inventoryRepository.findFirstByStockCodeIgnoreCase(normalized).ifPresent(matches::add);
        inventoryRepository.findFirstByStockNameIgnoreCase(normalized).ifPresent(matches::add);
        inventorySynonym.ifPresent(s -> {
            inventoryRepository.findFirstByStockCodeIgnoreCase(s.getActualValue()).ifPresent(matches::add);
            inventoryRepository.findFirstByStockNameIgnoreCase(s.getActualValue()).ifPresent(matches::add);
        });
        return matches.stream().toList();
    }

    private List<UnitEntity> exactUnitMatches(String query, Optional<SynonymEntity> unitSynonym) {
        LinkedHashSet<UnitEntity> matches = new LinkedHashSet<>();
        String normalized = query == null ? "" : query.trim();
        unitRepository.findFirstByStockUnitCodeIgnoreCase(normalized).ifPresent(matches::add);
        unitRepository.findFirstByStockUnitNameIgnoreCase(normalized).ifPresent(matches::add);
        unitSynonym.ifPresent(s -> {
            unitRepository.findFirstByStockUnitCodeIgnoreCase(s.getActualValue()).ifPresent(matches::add);
            unitRepository.findFirstByStockUnitNameIgnoreCase(s.getActualValue()).ifPresent(matches::add);
        });
        return matches.stream().toList();
    }

    private List<ResolvedCandidate> resolveUnits(String query,
                                                 InventoryEntity inventory,
                                                 double inventorySimilarity,
                                                 List<UnitEntity> exactUnitMatches,
                                                 List<UnitVectorMatch> unitMatches,
                                                 Optional<SynonymEntity> unitSynonym) {
        LinkedHashSet<UnitEntity> selectedUnits = new LinkedHashSet<>();
        selectedUnits.addAll(exactUnitMatches);
        unitMatches.stream()
                .flatMap(match -> unitRepository.findById(match.id()).stream())
                .forEach(selectedUnits::add);
        if (selectedUnits.isEmpty()) {
            unitRepository.findByActiveIgnoreCaseAndDeletedIgnoreCase("Y", "N").stream()
                    .limit(3)
                    .forEach(selectedUnits::add);
        }

        return selectedUnits.stream()
                .map(unit -> toCandidate(query, inventory, unit, inventorySimilarity, unitMatches, unitSynonym.isPresent()))
                .toList();
    }

    private ResolvedCandidate toCandidate(String query,
                                          InventoryEntity inventory,
                                          UnitEntity unit,
                                          double inventorySimilarity,
                                          List<UnitVectorMatch> unitMatches,
                                          boolean synonymMatched) {
        double unitSimilarity = unitMatches.stream()
                .filter(match -> match.id().equals(unit.getId()))
                .map(UnitVectorMatch::cosineSimilarity)
                .findFirst()
                .orElse(unitMatcher.queryMentionsUnit(query, unit) ? 1.0 : 0.70);
        boolean exact = stockMatcher.exactStockMatch(query, inventory);
        double confidence = confidenceCalculator.calculate(
                inventorySimilarity,
                unitSimilarity,
                stockMatcher.exactBoost(query, inventory),
                unitMatcher.unitBoost(query, unit),
                synonymMatched,
                true
        );
        String reason = exact ? "exact stock master match plus stock unit match" : "semantic stock master match plus stock unit match";
        return new ResolvedCandidate(inventory, unit, inventorySimilarity, unitSimilarity, exact, synonymMatched, true, confidence, reason);
    }
}
