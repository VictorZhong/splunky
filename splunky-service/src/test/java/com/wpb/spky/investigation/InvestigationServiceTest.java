package com.wpb.spky.investigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.investigation.InvestigationDtos.FollowUpRequest;
import com.wpb.spky.investigation.InvestigationDtos.FollowUpResponse;
import com.wpb.spky.investigation.InvestigationDtos.Investigation;
import com.wpb.spky.investigation.InvestigationDtos.StartInvestigationRequest;
import com.wpb.spky.investigation.InvestigationDtos.TimeRange;
import com.wpb.spky.investigation.InvestigationDtos.TimezoneOption;
import com.wpb.spky.llm.LlmRouter;
import com.wpb.spky.persistence.NoopAuditEventStore;
import com.wpb.spky.session.UserSession;
import com.wpb.spky.splunk.SplunkSearchRequest;
import com.wpb.spky.splunk.SplunkSearchResult;
import com.wpb.spky.splunk.SplunkSearcher;
import com.wpb.spky.splunk.SplunkUrlParser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvestigationServiceTest {

    @Test
    void createsFrontendCompatibleInvestigationFromFreeText() {
        FakeSplunkSearcher searcher = new FakeSplunkSearcher();
        InvestigationService service = service(searcher);

        Investigation investigation = service.start(session(), new StartInvestigationRequest(
                "Find payment-sapi logs for correlation id abc-123",
                List.of(),
                timeRange(),
                null
        ));

        assertThat(investigation.id()).startsWith("inv-");
        assertThat(investigation.input().correlationId()).isEqualTo("abc-123");
        assertThat(investigation.input().apiName()).isEqualTo("payment-sapi");
        assertThat(investigation.activeResult().queries()).hasSize(1);
        assertThat(investigation.activeResult().rawLogs()).hasSize(1);
        assertThat(investigation.activeResult().summary().rootCauseHypothesis()).contains("Result volume");
        assertThat(searcher.lastRequest.spl()).contains("abc-123");
    }

    @Test
    void importsSplunkUrlQuery() {
        FakeSplunkSearcher searcher = new FakeSplunkSearcher();
        InvestigationService service = service(searcher);

        Investigation investigation = service.start(session(), new StartInvestigationRequest(
                "https://digital-splunk-search.hk.zzzz/en-US/app/search/search?q=search%20index%3Dmain%20abc-123&earliest=-15m&latest=now",
                List.of(),
                timeRange(),
                null
        ));

        assertThat(investigation.input().detectedTypes()).contains("SPLUNK_URL");
        assertThat(searcher.lastRequest.spl()).isEqualTo("index=main abc-123");
        assertThat(searcher.lastRequest.earliest()).isEqualTo("-15m");
    }

    @Test
    void rerunFollowUpReplacesActiveResultAndAddsRun() {
        InvestigationService service = service(new FakeSplunkSearcher());
        Investigation investigation = service.start(session(), new StartInvestigationRequest(
                "correlation id abc-123",
                List.of("CORRELATION_ID"),
                timeRange(),
                null
        ));

        FollowUpResponse response = service.followUp(session(), investigation.id(),
                new FollowUpRequest("Expand to last 1 hour", null));

        assertThat(response.actionType()).isEqualTo("RERUN_QUERY");
        assertThat(response.investigation().runs()).hasSize(2);
        assertThat(response.investigation().activeResult().runNumber()).isEqualTo(2);
        assertThat(response.requeryStatus()).isNotNull();
    }

    private static InvestigationService service(FakeSplunkSearcher searcher) {
        ObjectMapper mapper = new ObjectMapper();
        LlmRouter router = new LlmRouter(List.of(), properties());
        return new InvestigationService(
                new SplunkQueryPlanner(router, mapper),
                new SplunkUrlParser(),
                searcher,
                new InvestigationSummaryService(router, mapper),
                new NoopAuditEventStore()
        );
    }

    private static UserSession session() {
        Instant now = Instant.now();
        return new UserSession(UUID.randomUUID(), UUID.randomUUID(), "zhong.zc",
                "secret-password", "SIT", UserSession.SessionStatus.ACTIVE,
                now, now.plusSeconds(1800), now);
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

    private static final class FakeSplunkSearcher implements SplunkSearcher {
        private SplunkSearchRequest lastRequest;

        @Override
        public SplunkSearchResult search(UserSession session, SplunkSearchRequest request) {
            this.lastRequest = request;
            return result("sid-123", request.sourceUrl());
        }

        @Override
        public SplunkSearchResult resultsForSid(UserSession session, String sid, String splunkUrl) {
            return result(sid, splunkUrl);
        }

        private static SplunkSearchResult result(String sid, String url) {
            return new SplunkSearchResult(sid, 1, 1, 0.2f, 123,
                    List.of(Map.of("_time", "2026-05-01T10:00:00Z",
                            "app", "payment-sapi",
                            "level", "ERROR",
                            "_raw", "payment-sapi failed for correlation id abc-123")),
                    url);
        }
    }
}
