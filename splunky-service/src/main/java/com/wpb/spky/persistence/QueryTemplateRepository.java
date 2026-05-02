package com.wpb.spky.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.wpb.spky.persistence.jpa.SpkyQueryTemplateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class QueryTemplateRepository {

    private final SpkyQueryTemplateJpaRepository queryTemplates;

    public List<QueryTemplateRecord> findEnabled() {
        return queryTemplates.findByEnabledTrueOrderByTemplateKeyAsc().stream()
                .map(entity -> new QueryTemplateRecord(
                        entity.getTemplateId(),
                        entity.getTemplateKey(),
                        entity.getName(),
                        entity.getDescription(),
                        entity.getTemplateSpl(),
                        entity.getDefaultTimeWindowMinutes(),
                        entity.getMaxTimeWindowMinutes(),
                        entity.getMaxResultCount(),
                        entity.isEnabled()
                ))
                .toList();
    }

    public record QueryTemplateRecord(
            UUID templateId,
            String templateKey,
            String name,
            String description,
            String templateSpl,
            int defaultTimeWindowMinutes,
            int maxTimeWindowMinutes,
            int maxResultCount,
            boolean enabled
    ) {}
}
