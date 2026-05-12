package com.asg.aiusecase.businesslogic;

import com.asg.aiusecase.entity.UnitEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UnitMatcher {

    public boolean queryMentionsUnit(String query, UnitEntity unit) {
        String normalized = normalize(query);
        return normalized.contains(normalize(unit.getUnitCode()))
                || normalized.contains(normalize(unit.getUnitName()));
    }

    public double unitBoost(String query, UnitEntity unit) {
        return queryMentionsUnit(query, unit) ? 0.12 : 0.0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
