package com.asg.aiusecase.businesslogic;

import com.asg.aiusecase.entity.SynonymEntity;
import com.asg.aiusecase.repository.SynonymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SynonymResolver {

    private final SynonymRepository synonymRepository;

    public List<SynonymEntity> findMatches(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return synonymRepository.findAll().stream()
                .filter(s -> normalized.contains(s.getSynonym().toLowerCase(Locale.ROOT)))
                .toList();
    }

    public Optional<SynonymEntity> findUnitSynonym(String query) {
        return findMatches(query).stream()
                .filter(s -> "UNIT".equalsIgnoreCase(s.getType()))
                .findFirst();
    }

    public Optional<SynonymEntity> findInventorySynonym(String query) {
        return findMatches(query).stream()
                .filter(s -> "INVENTORY".equalsIgnoreCase(s.getType()) || "STOCK".equalsIgnoreCase(s.getType()))
                .findFirst();
    }
}
