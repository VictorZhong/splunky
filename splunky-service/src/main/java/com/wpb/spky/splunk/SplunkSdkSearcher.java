package com.wpb.spky.splunk;

import com.splunk.Event;
import com.splunk.HttpException;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsArgs;
import com.splunk.ResultsReaderJson;
import com.splunk.Service;
import com.wpb.spky.config.SplunkProperties;
import com.wpb.spky.persistence.AuditEventStore;
import com.wpb.spky.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SplunkSdkSearcher implements SplunkSearcher {

    private static final Logger log = LoggerFactory.getLogger(SplunkSdkSearcher.class);
    private static final Duration SID_WAIT_TIMEOUT = Duration.ofSeconds(60);
    private static final Pattern UNSAFE_COMMAND = Pattern.compile(
            "\\|\\s*(delete|outputlookup|outputcsv|collect|script|sendemail|map|rest|dbxquery)(\\s|$)",
            Pattern.CASE_INSENSITIVE);

    private final SplunkSessionService sessions;
    private final SplunkProperties properties;
    private final AuditEventStore auditEvents;

    public SplunkSdkSearcher(SplunkSessionService sessions, SplunkProperties properties,
                             AuditEventStore auditEvents) {
        this.sessions = sessions;
        this.properties = properties;
        this.auditEvents = auditEvents;
    }

    @Override
    public SplunkSearchResult search(UserSession session, SplunkSearchRequest request) {
        validateSearch(request.spl());
        Instant started = Instant.now();
        try {
            Service service = sessions.getService(session);
            Job job = createJobWithAuthRetry(session, service, request);
            SplunkSearchResult result = readJobResults(job, started, request.sourceUrl());
            auditEvents.record(session, "SPLUNK_QUERY_EXECUTED", "SPLUNK_JOB", null,
                    "Splunk query completed", Map.of(
                            "sid", nullToEmpty(result.sid()),
                            "resultCount", result.resultCount(),
                            "durationMs", result.executionDurationMs(),
                            "splHash", sha256(request.spl())
                    ));
            return result;
        } catch (RuntimeException ex) {
            auditEvents.record(session, "SPLUNK_QUERY_FAILED", "SPLUNK_JOB", null,
                    "Splunk query failed", Map.of(
                            "errorType", ex.getClass().getSimpleName(),
                            "splHash", sha256(request.spl())
                    ));
            throw ex;
        }
    }

    @Override
    public SplunkSearchResult resultsForSid(UserSession session, String sid, String splunkUrl) {
        if (sid == null || sid.isBlank()) {
            throw new IllegalArgumentException("Splunk sid must not be blank.");
        }
        Instant started = Instant.now();
        try {
            Service service = sessions.getService(session);
            Job job = getJobWithAuthRetry(session, service, sid.trim());
            waitForSidIfNeeded(job);
            SplunkSearchResult result = readJobResults(job, started, splunkUrl);
            auditEvents.record(session, "SPLUNK_URL_IMPORTED", "SPLUNK_JOB", null,
                    "Splunk URL results imported", Map.of(
                            "sid", sid.trim(),
                            "resultCount", result.resultCount(),
                            "durationMs", result.executionDurationMs()
                    ));
            return result;
        } catch (RuntimeException ex) {
            auditEvents.record(session, "SPLUNK_URL_IMPORT_FAILED", "SPLUNK_JOB", null,
                    "Splunk URL import failed", Map.of(
                            "sid", sid.trim(),
                            "errorType", ex.getClass().getSimpleName()
                    ));
            throw ex;
        }
    }

    private Job createJobWithAuthRetry(UserSession session, Service service, SplunkSearchRequest request) {
        JobArgs args = new JobArgs();
        args.setEarliestTime(request.earliest());
        args.setLatestTime(request.latest());
        args.setExecutionMode(JobArgs.ExecutionMode.BLOCKING);

        int attempt = 0;
        Service localService = service;
        while (true) {
            try {
                String searchCommand = withSearchPrefix(request.spl());
                log.info("Creating Splunk job attempt={} staffId={} reason={}",
                        attempt + 1, session.username(), request.reason());
                Job job = localService.getJobs().create(searchCommand, args);
                job.control("setttl", Map.of("ttl", properties.jobTtlSecondsOrDefault()));
                log.info("Splunk job completed sid={} resultCount={} eventCount={} runDuration={}",
                        job.getSid(), job.getResultCountLong(), job.getEventCountLong(), job.getRunDuration());
                return job;
            } catch (HttpException ex) {
                if (isUnauthorized(ex) && attempt < properties.maxAuthRetryOrDefault()) {
                    attempt++;
                    sleepBackoff(attempt);
                    localService = sessions.relogin(session);
                    continue;
                }
                throw new SplunkConnectivityException("Error creating Splunk job", ex.getStatus(), ex);
            } catch (Exception ex) {
                throw new SplunkConnectivityException("Error creating Splunk job", ex);
            }
        }
    }

    private Job getJobWithAuthRetry(UserSession session, Service service, String sid) {
        int attempt = 0;
        Service localService = service;
        while (true) {
            try {
                Job job = localService.getJob(sid);
                if (job == null) {
                    throw new SplunkConnectivityException("Splunk job not found: " + sid, 404, null);
                }
                return job;
            } catch (HttpException ex) {
                if (isUnauthorized(ex) && attempt < properties.maxAuthRetryOrDefault()) {
                    attempt++;
                    sleepBackoff(attempt);
                    localService = sessions.relogin(session);
                    continue;
                }
                throw new SplunkConnectivityException("Error loading Splunk job", ex.getStatus(), ex);
            } catch (SplunkConnectivityException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new SplunkConnectivityException("Error loading Splunk job", ex);
            }
        }
    }

    private void waitForSidIfNeeded(Job job) {
        Instant deadline = Instant.now().plus(SID_WAIT_TIMEOUT);
        while (!job.isDone() && Instant.now().isBefore(deadline)) {
            sleep(1000);
            job.refresh();
        }
    }

    private SplunkSearchResult readJobResults(Job job, Instant started, String sourceUrl) {
        JobResultsArgs args = new JobResultsArgs();
        args.setOutputMode(JobResultsArgs.OutputMode.JSON);
        args.setCount(properties.resultRowLimitOrDefault());

        List<Map<String, String>> rows = new ArrayList<>();
        ResultsReaderJson reader = null;
        try (InputStream results = job.getResults(args)) {
            reader = new ResultsReaderJson(results);
            Event event;
            while ((event = reader.getNextEvent()) != null) {
                rows.add(Collections.unmodifiableMap(new LinkedHashMap<>(event)));
            }
        } catch (Exception ex) {
            throw new SplunkConnectivityException("Error reading Splunk results", ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    log.warn("Error closing Splunk results reader: {}", ex.getMessage());
                }
            }
        }
        return new SplunkSearchResult(
                job.getSid(),
                job.getEventCountLong(),
                job.getResultCountLong(),
                job.getRunDuration(),
                Duration.between(started, Instant.now()).toMillis(),
                List.copyOf(rows),
                sourceUrl == null || sourceUrl.isBlank() ? apiJobUrl(job.getSid()) : sourceUrl
        );
    }

    private String apiJobUrl(String sid) {
        if (sid == null || sid.isBlank()) return null;
        return properties.schemeOrDefault() + "://" + properties.hostOrDefault() + ":"
                + properties.portOrDefault() + "/services/search/jobs/" + sid;
    }

    private static void validateSearch(String spl) {
        if (spl == null || spl.isBlank()) {
            throw new IllegalArgumentException("Splunk search must not be blank.");
        }
        var matcher = UNSAFE_COMMAND.matcher(spl);
        if (matcher.find()) {
            throw new IllegalArgumentException("Unsafe Splunk command is not allowed in MVP: " + matcher.group(1));
        }
    }

    private static String withSearchPrefix(String spl) {
        String value = spl.trim();
        return value.regionMatches(true, 0, "search ", 0, "search ".length())
                ? value
                : "search " + value;
    }

    private static boolean isUnauthorized(HttpException ex) {
        return ex.getStatus() == 401 || "HTTP 401".equalsIgnoreCase(ex.getMessage());
    }

    private static void sleepBackoff(int attempt) {
        sleep(500L * attempt);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    (value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return "";
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
