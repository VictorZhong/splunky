package com.wpb.spky.api;

import com.wpb.spky.session.SessionCredentialManager;
import com.wpb.spky.session.SessionDtos.SessionResponse;
import com.wpb.spky.session.SessionDtos.SplunkLoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private static final String FRONTEND_SESSION_HEADER = "X-Splunky-Session-Id";
    private static final String API_SESSION_HEADER = "X-SPKY-Session-Id";

    private final SessionCredentialManager sessions;

    @PostMapping("/splunk-login")
    public SessionResponse login(@Valid @RequestBody SplunkLoginRequest request) {
        return sessions.start(request.splunkUsername(), request.splunkPassword(), request.environment());
    }

    @GetMapping("/current")
    public SessionResponse current(
            @RequestHeader(value = FRONTEND_SESSION_HEADER, required = false) String frontendSessionId,
            @RequestHeader(value = API_SESSION_HEADER, required = false) String apiSessionId) {
        String sessionId = SessionCredentialManager.resolveSessionId(frontendSessionId, apiSessionId);
        return sessions.find(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Missing or expired Splunky session."));
    }

    @DeleteMapping("/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestHeader(value = FRONTEND_SESSION_HEADER, required = false) String frontendSessionId,
            @RequestHeader(value = API_SESSION_HEADER, required = false) String apiSessionId) {
        sessions.logout(SessionCredentialManager.resolveSessionId(frontendSessionId, apiSessionId));
    }
}
