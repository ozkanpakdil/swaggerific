package io.github.ozkanpakdil.swaggerific.tools.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

/**
 * Simple request/response history storage with retention purge based on preferences.
 */
public class HistoryService {
    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);
    private static final Preferences prefs = Preferences.userNodeForPackage(SwaggerApplication.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final String KEY_SAVE_HISTORY = "ui.data.saveHistory";
    public static final String KEY_HISTORY_RETENTION_DAYS = "ui.data.historyRetentionDays";

    private static final Path BASE_DIR = Paths.get(MainController.APP_SETTINGS_HOME, "history");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    public static void ensureDir() {
        try {
            Files.createDirectories(BASE_DIR);
        } catch (IOException e) {
            log.warn("Cannot create history directory: {}", e.getMessage());
        }
    }

    /**
     * Save a history entry if enabled.
     */
    public static void save(String method, URI uri, Map<String, String> requestHeaders, String requestBody, HttpResponse response) {
        if (!prefs.getBoolean(KEY_SAVE_HISTORY, true)) {
            return;
        }
        ensureDir();
        try {
            String ts = TS_FMT.format(Instant.now());
            String safeMethod = method == null ? "UNKNOWN" : method;
            String safeHost = uri == null || uri.getHost() == null ? "unknown" : uri.getHost();
            String baseName = ts + "-" + safeMethod + "-" + safeHost;
            Path file = BASE_DIR.resolve(baseName + ".json");

            Map<String, Object> record = new HashMap<>();
            record.put("timestamp", Instant.now().toEpochMilli());
            record.put("method", safeMethod);
            record.put("uri", uri != null ? uri.toString() : "");
            record.put("requestHeaders", requestHeaders != null ? requestHeaders : Map.of());
            record.put("requestBody", requestBody);
            if (response != null) {
                record.put("status", response.statusCode());
                record.put("responseHeaders", response.headers());
                record.put("responseContentType", response.contentType());
                record.put("responseBody", response.body());
                record.put("error", response.isError() ? response.errorMessage() : null);
            }
            byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(record);
            Files.write(file, json);
        } catch (Exception e) {
            log.warn("Failed to save history: {}", e.getMessage());
        }
    }

    /**
     * Purge entries older than retention days preference.
     */
    public static int purgeOld() {
        ensureDir();
        int days = Math.max(0, prefs.getInt(KEY_HISTORY_RETENTION_DAYS, 30));
        long cutoffMillis = System.currentTimeMillis() - (long) days * 24L * 60L * 60L * 1000L;
        AtomicInteger deleted = new AtomicInteger(0);
        try {
            File dir = BASE_DIR.toFile();
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files == null) return 0;
            for (File f : files) {
                try {
                    long lastMod = f.lastModified();
                    if (lastMod < cutoffMillis) {
                        if (f.delete()) {
                            deleted.incrementAndGet();
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Failed to purge history: {}", e.getMessage());
        }
        return deleted.get();
    }

    public static Path getBaseDir() { return BASE_DIR; }
}
