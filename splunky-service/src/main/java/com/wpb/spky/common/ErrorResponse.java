package com.wpb.spky.common;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> details,
        Instant timestamp
) {}
