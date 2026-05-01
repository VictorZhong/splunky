package com.wpb.spky.investigation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlannedSplunkQuery(
        String spl,
        String earliest,
        String latest,
        String reason
) {}
