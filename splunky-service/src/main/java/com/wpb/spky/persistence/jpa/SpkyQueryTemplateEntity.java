package com.wpb.spky.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "spky_query_template")
@Getter
@Setter
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
}
