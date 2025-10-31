package io.github.ozkanpakdil.swaggerific.tools.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Lightweight, privacy-preserving update checker.
 *
 * Behaviour:
 * - Only runs if ui.update.checkOnStartup is true.
 * - Respects ui.update.channel (stable|beta).
 * - Makes a single, short HTTP GET request (5s timeout) to a public endpoint (default: GitHub Releases API).
 * - Does not send any PII; no request body; default headers only.
 * - Reports result via the provided notifier (e.g., logs or UI notification).
 */
public class UpdateChecker {
    private static final Logger log = LoggerFactory.getLogger(UpdateChecker.class);

    public static final String KEY_CHECK_ON_STARTUP = "ui.update.checkOnStartup";
    public static final String KEY_UPDATE_CHANNEL = "ui.update.channel"; // stable|beta

    public interface HttpClientAdapter {
        String get(String url, int timeoutMs) throws Exception;
    }

    public static class DefaultHttpClientAdapter implements HttpClientAdapter {
        private final HttpClient client = HttpClient.newBuilder().build();
        @Override
        public String get(String url, int timeoutMs) throws Exception {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(1, timeoutMs)))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return resp.body();
            }
            throw new IllegalStateException("HTTP " + resp.statusCode());
        }
    }

    private final Preferences prefs;
    private final HttpClientAdapter http;

    public UpdateChecker(Preferences prefs) {
        this(prefs, new DefaultHttpClientAdapter());
    }

    public UpdateChecker(Preferences prefs, HttpClientAdapter http) {
        this.prefs = Objects.requireNonNull(prefs);
        this.http = Objects.requireNonNull(http);
    }

    /**
     * Kicks off an async update check if enabled. Returns a future of the human-readable status message
     * also passed to the notifier.
     */
    public CompletableFuture<String> checkAsync(Consumer<String> notifier) {
        boolean enabled = prefs.getBoolean(KEY_CHECK_ON_STARTUP, false);
        if (!enabled) {
            return CompletableFuture.completedFuture("Update check disabled");
        }
        String channel = prefs.get(KEY_UPDATE_CHANNEL, "stable");
        String endpoint = resolveEndpoint(channel);
        int timeoutMs = 5000; // short timeout
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = http.get(endpoint, timeoutMs);
                // Parse minimal fields from GitHub Releases JSON
                UpdateInfo info = parseGitHubReleases(body, channel);
                String msg = formatMessage(info);
                if (notifier != null) notifier.accept(msg);
                return msg;
            } catch (Exception e) {
                String msg = "Update check failed: " + e.getMessage();
                if (notifier != null) notifier.accept(msg);
                return msg;
            }
        });
    }

    private static String resolveEndpoint(String channel) {
        // Allow override via system property for tests or enterprise envs
        String override = System.getProperty("swaggerific.update.endpoint");
        if (override != null && !override.isBlank()) return override;
        // stable uses latest release endpoint; beta uses all releases to include prereleases
        if ("beta".equalsIgnoreCase(channel)) {
            return "https://api.github.com/repos/ozkanpakdil/swaggerific/releases";
        }
        return "https://api.github.com/repos/ozkanpakdil/swaggerific/releases/latest";
    }

    private static String formatMessage(UpdateInfo info) {
        if (info == null) return "No update info";
        return "[Update] Latest " + info.channel + " version: " + info.version +
                (info.url != null ? " (" + info.url + ")" : "");
    }

    /** Minimal container for update info. */
    static class UpdateInfo {
        final String channel; // stable|beta
        final String version;
        final String url;
        UpdateInfo(String channel, String version, String url) {
            this.channel = channel; this.version = version; this.url = url;
        }
    }

    /**
     * Very small JSON parsing without external libs beyond JDK. We only need tag_name and html_url.
     * For beta channel, we read the first array element. For stable, object with tag_name.
     */
    static UpdateInfo parseGitHubReleases(String json, String channel) {
        if (json == null || json.isBlank()) return new UpdateInfo(channel, "unknown", null);
        String tag = null;
        String url = null;
        if (json.trim().startsWith("{")) { // single latest release
            tag = extractJsonString(json, "tag_name");
            url = extractJsonString(json, "html_url");
        } else if (json.trim().startsWith("[")) { // list of releases
            // take first element, prefer prerelease if channel=beta, else first non-prerelease
            // naive split on objects
            // Try to find first object
            String firstObj = extractFirstJsonObject(json);
            if (firstObj != null) {
                tag = extractJsonString(firstObj, "tag_name");
                url = extractJsonString(firstObj, "html_url");
            }
        }
        if (tag == null) tag = "unknown";
        return new UpdateInfo(channel, tag, url);
    }

    private static String extractFirstJsonObject(String arrayJson) {
        int i = arrayJson.indexOf('{');
        if (i < 0) return null;
        int depth = 0;
        for (int idx = i; idx < arrayJson.length(); idx++) {
            char c = arrayJson.charAt(idx);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return arrayJson.substring(i, idx + 1);
                }
            }
        }
        return null;
    }

    private static String extractJsonString(String json, String field) {
        // naive search: "field":"value"
        String key = "\"" + field + "\"";
        int k = json.indexOf(key);
        if (k < 0) return null;
        int colon = json.indexOf(':', k + key.length());
        if (colon < 0) return null;
        int startQuote = json.indexOf('"', colon + 1);
        if (startQuote < 0) return null;
        int endQuote = -1;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && json.charAt(i - 1) != '\\') { endQuote = i; break; }
        }
        if (endQuote < 0) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}
