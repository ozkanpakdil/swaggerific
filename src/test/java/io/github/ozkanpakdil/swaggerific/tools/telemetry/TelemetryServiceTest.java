package io.github.ozkanpakdil.swaggerific.tools.telemetry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

public class TelemetryServiceTest {

    static class StubHttp implements TelemetryService.HttpAdapter {
        final List<String> urls = new ArrayList<>();
        final List<String> bodies = new ArrayList<>();
        final AtomicInteger posts = new AtomicInteger(0);
        int statusToReturn = 202;
        @Override
        public CompletableFuture<Integer> postJsonAsync(String url, String json, int timeoutMs) {
            urls.add(url);
            bodies.add(json);
            posts.incrementAndGet();
            return CompletableFuture.completedFuture(statusToReturn);
        }
    }

    private final Preferences prefs = Preferences.userNodeForPackage(TelemetryServiceTest.class);

    @AfterEach
    public void cleanup() throws Exception {
        prefs.clear();
        System.clearProperty("swaggerific.telemetry.endpoint");
    }

    @Test
    public void disabledPreference_resultsInNoOp() throws Exception {
        prefs.putBoolean(TelemetryService.KEY_SEND_ANON, false);
        TelemetryService svc = new TelemetryService(prefs, new StubHttp());
        int code = svc.sendStartupAsync().get();
        Assertions.assertEquals(0, code);
    }

    @Test
    public void enabledWithEndpoint_sendsStartup_onceAnonIdIsCreated() throws Exception {
        System.setProperty("swaggerific.telemetry.endpoint", "http://example.invalid/telemetry");
        prefs.putBoolean(TelemetryService.KEY_SEND_ANON, true);
        StubHttp http = new StubHttp();
        TelemetryService svc = new TelemetryService(prefs, http);
        int code = svc.sendStartupAsync().get();
        Assertions.assertEquals(202, code);
        Assertions.assertEquals(1, http.posts.get());
        String anon = prefs.get(TelemetryService.KEY_ANON_ID, null);
        Assertions.assertNotNull(anon, "anonymousId should be persisted");
        // payload contains anonymousId and event
        String body = http.bodies.get(0);
        Assertions.assertTrue(body.contains("\"anonymousId\""));
        Assertions.assertTrue(body.contains("app_start"));
    }

    @Test
    public void sendRequest_includesMethodAndStatus() throws Exception {
        System.setProperty("swaggerific.telemetry.endpoint", "http://example.invalid/telemetry");
        prefs.putBoolean(TelemetryService.KEY_SEND_ANON, true);
        StubHttp http = new StubHttp();
        TelemetryService svc = new TelemetryService(prefs, http);
        int code = svc.sendRequestAsync("POST", 201).get();
        Assertions.assertEquals(202, code);
        String payload = http.bodies.get(0);
        Assertions.assertTrue(payload.contains("request_complete"));
        Assertions.assertTrue(payload.contains("POST"));
        Assertions.assertTrue(payload.contains("201"));
    }
}
