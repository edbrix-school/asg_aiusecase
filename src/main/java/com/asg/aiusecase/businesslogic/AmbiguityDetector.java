package com.asg.aiusecase.businesslogic;

import com.asg.aiusecase.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AmbiguityDetector {

    private final AppProperties properties;

    public boolean isAmbiguous(List<ResolvedCandidate> candidates) {
        if (candidates.isEmpty()) {
            return false;
        }
        List<ResolvedCandidate> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(ResolvedCandidate::confidence).reversed())
                .toList();
        ResolvedCandidate top = sorted.getFirst();
        if (top.confidence() < properties.getMatching().getHighConfidenceThreshold()) {
            return true;
        }
        if (sorted.size() == 1) {
            return false;
        }
        return top.confidence() - sorted.get(1).confidence() <= properties.getMatching().getAmbiguityDelta();
    }
}
