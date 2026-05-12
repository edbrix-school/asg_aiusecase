package com.asg.aiusecase.businesslogic;

import org.springframework.stereotype.Component;

@Component
public class ConfidenceCalculator {

    public double calculate(double inventorySimilarity,
                            double unitSimilarity,
                            double exactBoost,
                            double unitBoost,
                            boolean synonymMatched,
                            boolean compatible) {
        double score = (inventorySimilarity * 0.58)
                + (unitSimilarity * 0.22)
                + exactBoost
                + unitBoost
                + (synonymMatched ? 0.05 : 0.0)
                + (compatible ? 0.08 : -0.40);
        return Math.max(0.0, Math.min(1.0, score));
    }
}
