package com.wpb.spky.api;

import com.wpb.spky.investigation.InvestigationDtos.FollowUpRequest;
import com.wpb.spky.investigation.InvestigationDtos.FollowUpResponse;
import com.wpb.spky.investigation.InvestigationDtos.Investigation;
import com.wpb.spky.investigation.InvestigationDtos.StartInvestigationRequest;
import com.wpb.spky.investigation.InvestigationService;
import com.wpb.spky.session.SessionCredentialManager;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/investigations")
public class InvestigationController {

    private static final String FRONTEND_SESSION_HEADER = "X-Splunky-Session-Id";
    private static final String API_SESSION_HEADER = "X-SPKY-Session-Id";

    private final SessionCredentialManager sessions;
    private final InvestigationService investigations;

    public InvestigationController(SessionCredentialManager sessions, InvestigationService investigations) {
        this.sessions = sessions;
        this.investigations = investigations;
    }

    @PostMapping
    public Investigation start(
            @RequestHeader(value = FRONTEND_SESSION_HEADER, required = false) String frontendSessionId,
            @RequestHeader(value = API_SESSION_HEADER, required = false) String apiSessionId,
            @Valid @RequestBody StartInvestigationRequest request) {
        sessions.require(SessionCredentialManager.resolveSessionId(frontendSessionId, apiSessionId));
        return investigations.start(request);
    }

    @GetMapping("/{investigationId}")
    public Investigation get(
            @RequestHeader(value = FRONTEND_SESSION_HEADER, required = false) String frontendSessionId,
            @RequestHeader(value = API_SESSION_HEADER, required = false) String apiSessionId,
            @PathVariable String investigationId) {
        sessions.require(SessionCredentialManager.resolveSessionId(frontendSessionId, apiSessionId));
        return investigations.get(investigationId);
    }

    @PostMapping("/{investigationId}/follow-ups")
    public FollowUpResponse followUp(
            @RequestHeader(value = FRONTEND_SESSION_HEADER, required = false) String frontendSessionId,
            @RequestHeader(value = API_SESSION_HEADER, required = false) String apiSessionId,
            @PathVariable String investigationId,
            @Valid @RequestBody FollowUpRequest request) {
        sessions.require(SessionCredentialManager.resolveSessionId(frontendSessionId, apiSessionId));
        return investigations.followUp(investigationId, request);
    }
}
