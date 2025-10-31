package io.github.ozkanpakdil.swaggerific.tools.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

/**
 * Opt-in anonymous telemetry. Strictly no PII. Best-effort fire-and-forget with short timeouts.
 *
 * Behavior:
 * - Controlled by Preferences key analytics.sendAnonymousUsage (boolean). If false, everything is no-op.
 * - Generates and persists a random anonymousId (UUID v4) under analytics.anonymousId once enabled.
 * - Sends minimal JSON payloads to a configurable endpoint (system property swaggerific.telemetry.endpoint).
 *   Default endpoint disabled (no real network) unless overridden. In absence of endpoint, logs and no-ops.
 * - Provides helper events: sendStartupAsync() and sendRequestAsync(method, statusCode).
 */
public class TelemetryService {
    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    public static final String KEY_SEND_ANON = "analytics.sendAnonymousUsage"; // existing toggle from General
    public static final String KEY_ANON_ID = "analytics.anonymousId";

    /** HTTP adapter for testability */
    public interface HttpAdapter {
        CompletableFuture<Integer> postJsonAsync(String url, String json, int timeoutMs);
    }

    public static class JdkHttpAdapter implements HttpAdapter {
        private final HttpClient client = HttpClient.newBuilder().build();
        @Override
        public CompletableFuture<Integer> postJsonAsync(String url, String json, int timeoutMs) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMillis(Math.max(1, timeoutMs)))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                return client.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                        .thenApply(HttpResponse::statusCode)
                        .exceptionally(ex -> {
                            log.debug("Telemetry POST failed: {}", ex.toString());
                            return -1;
                        });
            } catch (Exception e) {
                log.debug("Telemetry POST build failed: {}", e.toString());
                return CompletableFuture.completedFuture(-1);
            }
        }
    }

    private final Preferences prefs;
    private final HttpAdapter http;

    public TelemetryService(Preferences prefs, HttpAdapter http) {
        this.prefs = Objects.requireNonNull(prefs);
        this.http = Objects.requireNonNull(http);
    }

    public TelemetryService(Preferences prefs) {
        this(prefs, new JdkHttpAdapter());
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_SEND_ANON, false);
    }

    private String getOrCreateAnonId() {
        String id = prefs.get(KEY_ANON_ID, null);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            try { prefs.put(KEY_ANON_ID, id); prefs.flush(); } catch (Exception ignored) {}
        }
        return id;
    }

    private String resolveEndpoint() {
        // Allow full override. Default is empty to avoid accidental outbound calls in dev/tests.
        return System.getProperty("swaggerific.telemetry.endpoint", "");
    }

    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            Object v = e.getValue();
            if (v == null) { sb.append("null"); }
            else if (v instanceof Number || v instanceof Boolean) { sb.append(v.toString()); }
            else { sb.append('"').append(escape(String.valueOf(v))).append('"'); }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Map<String, Object> basePayload() {
        Map<String, Object> m = new HashMap<>();
        m.put("anonymousId", getOrCreateAnonId());
        m.put("appVersion", readProp("/application.properties", "app.version", "0"));
        m.put("java", System.getProperty("java.version", "unknown"));
        m.put("os", System.getProperty("os.name", "unknown"));
        m.put("osVersion", System.getProperty("os.version", "unknown"));
        return m;
    }

    private static String readProp(String res, String key, String def) {
        try (var in = TelemetryService.class.getResourceAsStream(res)) {
            if (in != null) {
                java.util.Properties p = new java.util.Properties();
                p.load(in);
                return p.getProperty(key, def);
            }
        } catch (Exception ignored) {}
        return def;
    }

    public CompletableFuture<Integer> sendStartupAsync() {
        if (!isEnabled()) return CompletableFuture.completedFuture(0);
        String endpoint = resolveEndpoint();
        if (endpoint.isBlank()) return CompletableFuture.completedFuture(0);
        Map<String, Object> m = basePayload();
        m.put("event", "app_start");
        m.put("ts", System.currentTimeMillis());
        return http.postJsonAsync(endpoint, toJson(m), 3000);
    }

    public CompletableFuture<Integer> sendRequestAsync(String method, int statusCode) {
        if (!isEnabled()) return CompletableFuture.completedFuture(0);
        String endpoint = resolveEndpoint();
        if (endpoint.isBlank()) return CompletableFuture.completedFuture(0);
        Map<String, Object> m = basePayload();
        m.put("event", "request_complete");
        m.put("ts", System.currentTimeMillis());
        if (method != null) m.put("method", method);
        m.put("status", statusCode);
        return http.postJsonAsync(endpoint, toJson(m), 3000);
    }
}
