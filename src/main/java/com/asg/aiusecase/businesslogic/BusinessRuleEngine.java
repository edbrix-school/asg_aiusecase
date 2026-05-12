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
    private final CompatibilityValidator compatibilityValidator;
    private final ConfidenceCalculator confidenceCalculator;

    @Transactional(readOnly = true)
    public List<ResolvedCandidate> evaluate(String query,
                                            List<InventoryVectorMatch> inventoryMatches,
                                            List<UnitVectorMatch> unitMatches) {
        Optional<SynonymEntity> inventorySynonym = synonymResolver.findInventorySynonym(query);
        Optional<SynonymEntity> unitSynonym = synonymResolver.findUnitSynonym(query);

        List<InventoryEntity> exactInventoryMatches = exactInventoryMatches(query, inventorySynonym);
        List<ResolvedCandidate> vectorCandidates = inventoryMatches.stream()
                .flatMap(match -> inventoryRepository.findById(match.id()).stream()
                        .flatMap(inventory -> resolveUnitsForInventory(query, inventory, match.cosineSimilarity(), unitMatches, unitSynonym).stream()))
                .toList();

        List<ResolvedCandidate> exactCandidates = exactInventoryMatches.stream()
                .flatMap(inventory -> resolveUnitsForInventory(query, inventory, 1.0, unitMatches, unitSynonym).stream())
                .toList();

        return java.util.stream.Stream.concat(exactCandidates.stream(), vectorCandidates.stream())
                .filter(ResolvedCandidate::compatible)
                .collect(java.util.stream.Collectors.toMap(
                        c -> c.inventory().getId() + ":" + (c.unit() == null ? "null" : c.unit().getId()),
                        c -> c,
                        (left, right) -> left.confidence() >= right.confidence() ? left : right
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(ResolvedCandidate::confidence).reversed())
                .toList();
    }

    private List<InventoryEntity> exactInventoryMatches(String query, Optional<SynonymEntity> inventorySynonym) {
        java.util.LinkedHashSet<InventoryEntity> matches = new java.util.LinkedHashSet<>();
        String normalized = query == null ? "" : query.trim();
        inventoryRepository.findFirstByStockCodeIgnoreCase(normalized).ifPresent(matches::add);
        inventoryRepository.findFirstByProductCodeIgnoreCase(normalized).ifPresent(matches::add);
        inventorySynonym.ifPresent(s -> {
            inventoryRepository.findFirstByStockCodeIgnoreCase(s.getActualValue()).ifPresent(matches::add);
            inventoryRepository.findFirstByProductCodeIgnoreCase(s.getActualValue()).ifPresent(matches::add);
            inventoryRepository.findAll().stream()
                    .filter(i -> i.getProductName().equalsIgnoreCase(s.getActualValue()))
                    .findFirst()
                    .ifPresent(matches::add);
        });
        return matches.stream().toList();
    }

    private List<ResolvedCandidate> resolveUnitsForInventory(String query,
                                                             InventoryEntity inventory,
                                                             double inventorySimilarity,
                                                             List<UnitVectorMatch> unitMatches,
                                                             Optional<SynonymEntity> unitSynonym) {
        List<UnitEntity> compatibleUnits = compatibilityValidator.validRules(inventory.getId()).stream()
                .map(rule -> rule.getUnit())
                .toList();
        if (compatibleUnits.isEmpty()) {
            return List.of();
        }

        List<UnitEntity> mentionedCompatibleUnits = compatibleUnits.stream()
                .filter(unit -> unitMatcher.queryMentionsUnit(query, unit)
                        || unitSynonym.map(s -> unit.getUnitCode().equalsIgnoreCase(s.getActualValue())
                        || unit.getUnitName().equalsIgnoreCase(s.getActualValue())).orElse(false))
                .toList();

        List<UnitEntity> vectorCompatibleUnits = unitMatches.stream()
                .filter(match -> match.inventoryId().equals(inventory.getId()))
                .flatMap(match -> unitRepository.findById(match.id()).stream())
                .filter(unit -> compatibilityValidator.isCompatible(inventory.getId(), unit.getId()))
                .toList();

        java.util.LinkedHashSet<UnitEntity> selectedUnits = new java.util.LinkedHashSet<>();
        selectedUnits.addAll(mentionedCompatibleUnits);
        selectedUnits.addAll(vectorCompatibleUnits);
        if (selectedUnits.isEmpty()) {
            compatibilityValidator.highestPriorityValidUnit(inventory.getId()).ifPresent(selectedUnits::add);
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
        boolean compatible = compatibilityValidator.isCompatible(inventory.getId(), unit.getId());
        boolean exact = stockMatcher.exactStockMatch(query, inventory);
        double confidence = confidenceCalculator.calculate(
                inventorySimilarity,
                unitSimilarity,
                stockMatcher.exactBoost(query, inventory),
                unitMatcher.unitBoost(query, unit),
                synonymMatched,
                compatible
        );
        String reason = exact ? "exact stock/product code plus business-compatible unit" : "semantic vector match plus business-compatible unit";
        return new ResolvedCandidate(inventory, unit, inventorySimilarity, unitSimilarity, exact, synonymMatched, compatible, confidence, reason);
    }
}
