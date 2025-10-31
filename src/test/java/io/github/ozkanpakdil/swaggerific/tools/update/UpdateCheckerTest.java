package io.github.ozkanpakdil.swaggerific.tools.update;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

public class UpdateCheckerTest {

    static class StubHttp implements UpdateChecker.HttpClientAdapter {
        private final String body;
        private final RuntimeException toThrow;
        StubHttp(String body) { this.body = body; this.toThrow = null; }
        StubHttp(RuntimeException ex) { this.body = null; this.toThrow = ex; }
        @Override public String get(String url, int timeoutMs) {
            if (toThrow != null) throw toThrow;
            return body;
        }
    }

    @Test
    public void disabledPreference_returnsDisabledMessage() throws Exception {
        Preferences prefs = Preferences.userNodeForPackage(UpdateCheckerTest.class);
        prefs.putBoolean(UpdateChecker.KEY_CHECK_ON_STARTUP, false);
        UpdateChecker checker = new UpdateChecker(prefs, new StubHttp("{}"));
        String msg = checker.checkAsync(null).get();
        Assertions.assertTrue(msg.toLowerCase().contains("disabled"));
    }

    @Test
    public void stableChannel_parsesLatestObject() throws Exception {
        Preferences prefs = Preferences.userNodeForPackage(UpdateCheckerTest.class);
        prefs.putBoolean(UpdateChecker.KEY_CHECK_ON_STARTUP, true);
        prefs.put(UpdateChecker.KEY_UPDATE_CHANNEL, "stable");
        String json = "{\n  \"tag_name\": \"v1.2.3\",\n  \"html_url\": \"https://example/releases/v1.2.3\"\n}";
        AtomicReference<String> notified = new AtomicReference<>();
        UpdateChecker checker = new UpdateChecker(prefs, new StubHttp(json));
        String msg = checker.checkAsync(notified::set).get();
        Assertions.assertTrue(msg.contains("v1.2.3"), "Message should include version");
        Assertions.assertNotNull(notified.get(), "Notifier should be called");
    }

    @Test
    public void betaChannel_parsesFirstArrayItem() throws Exception {
        Preferences prefs = Preferences.userNodeForPackage(UpdateCheckerTest.class);
        prefs.putBoolean(UpdateChecker.KEY_CHECK_ON_STARTUP, true);
        prefs.put(UpdateChecker.KEY_UPDATE_CHANNEL, "beta");
        String json = "[ { \"tag_name\": \"v2.0.0-beta1\", \"html_url\": \"https://example/beta1\" }, { \"tag_name\": \"v1.9.0\" } ]";
        UpdateChecker checker = new UpdateChecker(prefs, new StubHttp(json));
        String msg = checker.checkAsync(null).get();
        Assertions.assertTrue(msg.contains("v2.0.0-beta1"));
    }

    @Test
    public void httpFailure_isReported() throws Exception {
        Preferences prefs = Preferences.userNodeForPackage(UpdateCheckerTest.class);
        prefs.putBoolean(UpdateChecker.KEY_CHECK_ON_STARTUP, true);
        prefs.put(UpdateChecker.KEY_UPDATE_CHANNEL, "stable");
        UpdateChecker checker = new UpdateChecker(prefs, new StubHttp(new RuntimeException("boom")));
        String msg = checker.checkAsync(null).get();
        Assertions.assertTrue(msg.toLowerCase().contains("failed"));
    }
}
