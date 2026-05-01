package com.wpb.spky.investigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.investigation.InvestigationDtos.TimeRange;
import com.wpb.spky.investigation.InvestigationDtos.TimezoneOption;
import com.wpb.spky.llm.LlmRouter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SplunkQueryPlannerTest {

    @Test
    void fallsBackToBoundedCorrelationSearchWhenLlmUnavailable() {
        SplunkQueryPlanner planner = new SplunkQueryPlanner(new LlmRouter(List.of(), properties()), new ObjectMapper());

        var request = planner.plan("Find logs for correlation id abc-123", timeRange());

        assertThat(request.spl()).contains("\"abc-123\"");
        assertThat(request.spl()).contains("| head 200");
        assertThat(request.earliest()).isNotBlank();
        assertThat(request.latest()).isNotBlank();
    }

    private static TimeRange timeRange() {
        Instant now = Instant.now();
        return new TimeRange("Last 30 min", now.minusSeconds(1800).toString(), now.toString(),
                new TimezoneOption("HKT", "+08:00"));
    }

    private static SplunkyProperties properties() {
        return new SplunkyProperties(
                new SplunkyProperties.PersistenceProperties(false),
                new SplunkyProperties.CryptoProperties(null),
                new SplunkyProperties.SessionProperties(30, true),
                null
        );
    }
}
