package com.wpb.spky.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "spky_query_template")
public class SpkyQueryTemplateEntity {

    @Id
    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_key", nullable = false)
    private String templateKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "template_spl", nullable = false)
    private String templateSpl;

    @Column(name = "default_time_window_minutes", nullable = false)
    private int defaultTimeWindowMinutes;

    @Column(name = "max_time_window_minutes", nullable = false)
    private int maxTimeWindowMinutes;

    @Column(name = "max_result_count", nullable = false)
    private int maxResultCount;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTemplateSpl() { return templateSpl; }
    public void setTemplateSpl(String templateSpl) { this.templateSpl = templateSpl; }
    public int getDefaultTimeWindowMinutes() { return defaultTimeWindowMinutes; }
    public void setDefaultTimeWindowMinutes(int defaultTimeWindowMinutes) { this.defaultTimeWindowMinutes = defaultTimeWindowMinutes; }
    public int getMaxTimeWindowMinutes() { return maxTimeWindowMinutes; }
    public void setMaxTimeWindowMinutes(int maxTimeWindowMinutes) { this.maxTimeWindowMinutes = maxTimeWindowMinutes; }
    public int getMaxResultCount() { return maxResultCount; }
    public void setMaxResultCount(int maxResultCount) { this.maxResultCount = maxResultCount; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
