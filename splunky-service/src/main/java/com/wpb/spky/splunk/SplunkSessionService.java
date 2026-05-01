package com.wpb.spky.splunk;

import com.splunk.HttpService;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import com.wpb.spky.config.SplunkProperties;
import com.wpb.spky.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SplunkSessionService {

    private static final Logger log = LoggerFactory.getLogger(SplunkSessionService.class);
    private static final AtomicBoolean TRUST_ALL_SSL_CONFIGURED = new AtomicBoolean(false);

    private final SplunkProperties properties;
    private final ConcurrentMap<UUID, Service> services = new ConcurrentHashMap<>();

    public SplunkSessionService(SplunkProperties properties) {
        this.properties = properties;
    }

    public Service getService(UserSession session) {
        requireSplunkCredential(session);
        return services.computeIfAbsent(session.sessionId(), ignored -> connect(session));
    }

    public Service relogin(UserSession session) {
        requireSplunkCredential(session);
        return services.compute(session.sessionId(), (ignored, old) -> connect(session));
    }

    public void invalidate(UUID sessionId) {
        if (sessionId != null) services.remove(sessionId);
    }

    @SuppressWarnings("java:S5527")
    private Service connect(UserSession session) {
        configureTrustAllSslIfNeeded();
        long started = System.currentTimeMillis();
        ServiceArgs args = new ServiceArgs();
        args.setUsername(session.username());
        args.setPassword(session.splunkPassword());
        args.setScheme(properties.schemeOrDefault());
        args.setHost(properties.hostOrDefault());
        args.setPort(properties.portOrDefault());

        try {
            log.info("Initialising Splunk service for host={} staffId={}",
                    properties.hostOrDefault(), session.username());
            Service service = Service.connect(args);
            service.login();
            log.info("Splunk login succeeded for staffId={} in {}ms",
                    session.username(), System.currentTimeMillis() - started);
            return service;
        } catch (Exception ex) {
            log.error("Splunk login failed for staffId={} host={}: {}",
                    session.username(), properties.hostOrDefault(), ex.getMessage(), ex);
            throw new SplunkConnectivityException("Error occurred in Splunk login", ex);
        }
    }

    private void requireSplunkCredential(UserSession session) {
        if (session == null || session.splunkPassword() == null || session.splunkPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Splunk credentials are required. Please log in with Splunk username and password.");
        }
    }

    @SuppressWarnings("java:S5527")
    private void configureTrustAllSslIfNeeded() {
        if (!properties.trustAllSslOrDefault()) return;
        if (!TRUST_ALL_SSL_CONFIGURED.compareAndSet(false, true)) return;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }
                    }
            };
            sslContext.init(null, trustManagers, new SecureRandom());
            HttpService.setSSLSocketFactory(sslContext.getSocketFactory());
            HttpService.setValidateCertificates(false);
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            log.warn("Splunk trust-all SSL is enabled for internal Splunk connectivity.");
        } catch (Exception ex) {
            TRUST_ALL_SSL_CONFIGURED.set(false);
            throw new SplunkConnectivityException("Failed to configure Splunk SSL trust manager", ex);
        }
    }
}
