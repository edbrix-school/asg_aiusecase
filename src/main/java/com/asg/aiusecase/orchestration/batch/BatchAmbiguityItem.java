package com.asg.aiusecase.orchestration.batch;

import com.asg.aiusecase.businesslogic.ResolvedCandidate;

import java.util.List;

public record BatchAmbiguityItem(
        String lineId,
        String query,
        List<ResolvedCandidate> candidates,
        String chatModel,
        String fallbackModel
) {
}
