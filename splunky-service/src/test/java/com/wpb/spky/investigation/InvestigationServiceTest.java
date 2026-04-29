package com.wpb.spky.investigation;

import com.wpb.spky.investigation.InvestigationDtos.FollowUpRequest;
import com.wpb.spky.investigation.InvestigationDtos.FollowUpResponse;
import com.wpb.spky.investigation.InvestigationDtos.Investigation;
import com.wpb.spky.investigation.InvestigationDtos.StartInvestigationRequest;
import com.wpb.spky.investigation.InvestigationDtos.TimeRange;
import com.wpb.spky.investigation.InvestigationDtos.TimezoneOption;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvestigationServiceTest {

    @Test
    void createsFrontendCompatibleInvestigation() {
        InvestigationService service = new InvestigationService();
        Investigation investigation = service.start(new StartInvestigationRequest(
                "Find payment-sapi logs for correlation id abc-123",
                List.of(),
                timeRange(),
                null
        ));

        assertThat(investigation.id()).startsWith("inv-");
        assertThat(investigation.input().correlationId()).isEqualTo("abc-123");
        assertThat(investigation.input().apiName()).isEqualTo("payment-sapi");
        assertThat(investigation.activeResult().queries()).hasSize(1);
        assertThat(investigation.conversation()).hasSize(2);
    }

    @Test
    void rerunFollowUpReplacesActiveResultAndAddsRun() {
        InvestigationService service = new InvestigationService();
        Investigation investigation = service.start(new StartInvestigationRequest(
                "correlation id abc-123",
                List.of("CORRELATION_ID"),
                timeRange(),
                null
        ));

        FollowUpResponse response = service.followUp(investigation.id(),
                new FollowUpRequest("Expand to last 1 hour", null));

        assertThat(response.actionType()).isEqualTo("RERUN_QUERY");
        assertThat(response.investigation().runs()).hasSize(2);
        assertThat(response.investigation().activeResult().runNumber()).isEqualTo(2);
        assertThat(response.requeryStatus()).isNotNull();
    }

    private static TimeRange timeRange() {
        Instant now = Instant.now();
        return new TimeRange("Last 30 min", now.minusSeconds(1800).toString(), now.toString(),
                new TimezoneOption("HKT", "+08:00"));
    }
}
