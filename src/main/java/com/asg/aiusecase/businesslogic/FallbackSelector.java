package com.asg.aiusecase.businesslogic;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class FallbackSelector {

    public Optional<ResolvedCandidate> selectBestBusinessValidCandidate(List<ResolvedCandidate> candidates) {
        return candidates.stream()
                .filter(ResolvedCandidate::compatible)
                .max(Comparator.comparingDouble(ResolvedCandidate::confidence));
    }
}
