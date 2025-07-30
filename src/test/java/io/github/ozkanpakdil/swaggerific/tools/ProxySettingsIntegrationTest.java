package io.github.ozkanpakdil.swaggerific.tools;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProxySettingsIntegrationTest {
    private static final String PROXY_USERNAME = "username";
    private static final String PROXY_PASSWORD = "password";
    private static final String TEST_URL = "https://petstore.swagger.io/v2/swagger.json";
    private static final Logger log = LoggerFactory.getLogger(ProxySettingsIntegrationTest.class);

    // Mock proxy server details for testing
    private static final String MOCK_PROXY_HOST = "127.0.0.1";
    private static final int MOCK_PROXY_PORT = 8080;

    @BeforeAll
    static void setUp() {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("app.environment", "development");
    }

    private HttpClient createProxyClient() throws Exception {
        // Create a trust manager that trusts all certificates
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Create SSL context that uses our trust manager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        // Configure the proxy selector
        var proxy = ProxySettings.createProxy();
        var proxyAddress = (InetSocketAddress) proxy.address();
        var proxySelector = ProxySelector.of(proxyAddress);

        // Configure basic proxy authentication
        java.net.Authenticator.setDefault(ProxySettings.createProxyAuthenticator());

        return HttpClient.newBuilder()
                .proxy(proxySelector)
                .sslContext(sslContext)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    @Test
    void testProxyAuthenticationSuccess() throws Exception {
        // Configure proxy settings
        ProxySettings.saveSettings(
                false,
                "HTTP",
                MOCK_PROXY_HOST,
                MOCK_PROXY_PORT,
                true,
                PROXY_USERNAME,
                PROXY_PASSWORD,
                "localhost",
                false
        );

        // Test that proxy settings are saved correctly
        assertEquals("HTTP", ProxySettings.getProxyType());
        assertEquals(MOCK_PROXY_HOST, ProxySettings.getProxyServer());
        assertEquals(MOCK_PROXY_PORT, ProxySettings.getProxyPort());
        assertTrue(ProxySettings.useProxyAuth());
        assertEquals(PROXY_USERNAME, ProxySettings.getProxyAuthUsername());

        // Test proxy creation
        var proxy = ProxySettings.createProxy();
        assertNotNull(proxy);
        assertEquals(Proxy.Type.HTTP, proxy.type());

        var proxyAddress = (InetSocketAddress) proxy.address();
        assertEquals(MOCK_PROXY_HOST, proxyAddress.getHostString());
        assertEquals(MOCK_PROXY_PORT, proxyAddress.getPort());

        // Test proxy authenticator creation
        var authenticator = ProxySettings.createProxyAuthenticator();
        assertNotNull(authenticator);

        // Test proxy authorization header
        String proxyAuth = ProxySettings.getProxyAuthorizationHeader();
        assertNotNull(proxyAuth);
        assertTrue(proxyAuth.startsWith("Basic "));
    }

    @Test
    void testProxyAuthenticationFailure() throws Exception {
        // Configure proxy settings with wrong password
        ProxySettings.saveSettings(
                false,
                "HTTP",
                MOCK_PROXY_HOST,
                MOCK_PROXY_PORT,
                true,
                PROXY_USERNAME,
                "wrongpassword",
                "localhost",
                false
        );

        // Test that proxy settings are saved correctly with wrong password
        assertEquals("HTTP", ProxySettings.getProxyType());
        assertEquals(MOCK_PROXY_HOST, ProxySettings.getProxyServer());
        assertEquals(MOCK_PROXY_PORT, ProxySettings.getProxyPort());
        assertTrue(ProxySettings.useProxyAuth());
        assertEquals(PROXY_USERNAME, ProxySettings.getProxyAuthUsername());

        // Test that proxy authorization header is generated (even with wrong password)
        String proxyAuth = ProxySettings.getProxyAuthorizationHeader();
        assertNotNull(proxyAuth);
        assertTrue(proxyAuth.startsWith("Basic "));

        // Verify the password is stored (encrypted)
        char[] storedPassword = ProxySettings.getProxyAuthPassword();
        assertNotNull(storedPassword);
        assertEquals("wrongpassword", new String(storedPassword));
    }

    @Test
    void testMissingProxySettings() {
        // Configure proxy settings with empty server
        Exception exception = assertThrows(IllegalStateException.class, () -> ProxySettings.saveSettings(
                false,
                "HTTP",
                "",
                3128,
                true,
                PROXY_USERNAME,
                PROXY_PASSWORD,
                "localhost",
                false
        ));

        assertTrue(exception.getMessage().contains("Proxy server cannot be empty"));
    }

    @Test
    void testMainApplicationProxyFlow() throws Exception {
        // Configure proxy settings
        ProxySettings.saveSettings(
                false,
                "HTTP",
                MOCK_PROXY_HOST,
                MOCK_PROXY_PORT,
                true,
                PROXY_USERNAME,
                PROXY_PASSWORD,
                "localhost",
                false
        );

        // Test proxy bypass functionality
        assertFalse(ProxySettings.shouldBypassProxy("example.com"));
        assertTrue(ProxySettings.shouldBypassProxy("localhost"));

        // Test proxy creation and configuration
        var proxy = ProxySettings.createProxy();
        assertNotNull(proxy);
        assertEquals(Proxy.Type.HTTP, proxy.type());

        var proxyAddress = (InetSocketAddress) proxy.address();
        assertEquals(MOCK_PROXY_HOST, proxyAddress.getHostString());
        assertEquals(MOCK_PROXY_PORT, proxyAddress.getPort());

        // Test that authenticator can be created
        var authenticator = ProxySettings.createProxyAuthenticator();
        assertNotNull(authenticator);

        // Test proxy selector creation
        var proxySelector = ProxySelector.of(proxyAddress);
        assertNotNull(proxySelector);

        // Test that HTTP client can be built with proxy settings
        HttpClient client = HttpClient.newBuilder()
                .proxy(proxySelector)
                .version(HttpClient.Version.HTTP_2)
                .authenticator(authenticator)
                .build();

        assertNotNull(client);
    }

    @Test
    void testProxy() throws Exception {
        // Configure proxy settings
        ProxySettings.saveSettings(
                false,
                "HTTP",
                MOCK_PROXY_HOST,
                MOCK_PROXY_PORT,
                true,
                PROXY_USERNAME,
                PROXY_PASSWORD,
                "localhost",
                false
        );

        // Test proxy settings validation
        assertDoesNotThrow(ProxySettings::validateProxySettings);

        // Test proxy creation
        var proxy = ProxySettings.createProxy();
        assertNotNull(proxy);
        assertEquals(Proxy.Type.HTTP, proxy.type());

        var proxyAddress = (InetSocketAddress) proxy.address();
        assertEquals(MOCK_PROXY_HOST, proxyAddress.getHostString());
        assertEquals(MOCK_PROXY_PORT, proxyAddress.getPort());

        // Test authenticator creation
        Authenticator authenticator = ProxySettings.createProxyAuthenticator();
        assertNotNull(authenticator);

        // Test SSL context creation
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        assertNotNull(sslContext);

        // Test HTTP client creation with all proxy components
        var proxySelector = ProxySelector.of(proxyAddress);
        HttpClient client = HttpClient.newBuilder()
                .proxy(proxySelector)
                .sslContext(sslContext)
                .authenticator(authenticator)
                .version(HttpClient.Version.HTTP_2)
                .build();

        assertNotNull(client);
        log.info("Proxy configuration test completed successfully");
    }
}
