package com.wpb.spky.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class QueryTemplateRepository {

    private final JdbcClient jdbc;

    public QueryTemplateRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<QueryTemplateRecord> findEnabled() {
        return jdbc.sql("""
                select template_id, template_key, name, description, template_spl,
                       default_time_window_minutes, max_time_window_minutes, max_result_count, enabled
                from spky_query_template
                where enabled = true
                order by template_key
                """)
                .query((rs, rowNum) -> new QueryTemplateRecord(
                        rs.getObject("template_id", UUID.class),
                        rs.getString("template_key"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("template_spl"),
                        rs.getInt("default_time_window_minutes"),
                        rs.getInt("max_time_window_minutes"),
                        rs.getInt("max_result_count"),
                        rs.getBoolean("enabled")
                ))
                .list();
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
