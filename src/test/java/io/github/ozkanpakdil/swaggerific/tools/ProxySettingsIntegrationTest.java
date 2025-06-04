package io.github.ozkanpakdil.swaggerific.tools;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class ProxySettingsIntegrationTest {
    private static final String PROXY_USERNAME = "username";
    private static final String PROXY_PASSWORD = "password";
    private static final String TEST_URL = "https://petstore.swagger.io/v2/swagger.json";
    private static final Logger log = LoggerFactory.getLogger(ProxySettingsIntegrationTest.class);

    @Container
    public static GenericContainer<?> squidContainer = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", "proxy/Dockerfile")
                    .withFileFromClasspath("entrypoint.sh", "proxy/entrypoint.sh")
                    .withFileFromClasspath("squid.conf", "proxy/squid.conf"))
            .withExposedPorts(3128)
            .withEnv("PROXY_USERNAME", PROXY_USERNAME)
            .withEnv("PROXY_PASSWORD", PROXY_PASSWORD);

    @BeforeAll
    static void setUp() {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("app.environment", "development");
    }

    private HttpClient createProxyClient() throws Exception {
        // Create a trust manager that trusts all certificates
        TrustManager[] trustAllCerts = new TrustManager[]{
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
                "127.0.0.1",
                squidContainer.getMappedPort(3128),
                true,
                PROXY_USERNAME,
                PROXY_PASSWORD,
                "localhost",
                false
        );

        // Get proxy authorization header first
        String proxyAuth = ProxySettings.getProxyAuthorizationHeader();

        // Create new authenticator
        java.net.Authenticator.setDefault(new java.net.Authenticator() {
            @Override
            protected java.net.PasswordAuthentication getPasswordAuthentication() {
                return new java.net.PasswordAuthentication(
                        PROXY_USERNAME,
                        PROXY_PASSWORD.toCharArray()
                );
            }
        });

        // Create HTTP client with proxy settings
        var proxy = ProxySettings.createProxy();
        var proxyAddress = (InetSocketAddress) proxy.address();
        var proxySelector = ProxySelector.of(proxyAddress);

        HttpClient client = HttpClient.newBuilder()
                .proxy(proxySelector)
                .authenticator(java.net.Authenticator.getDefault())
                .version(HttpClient.Version.HTTP_2)
                .build();

        // Make request with explicit proxy auth header
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://httpbin.org/status/200"))
                .header("Proxy-Authorization", proxyAuth)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Should get 200 OK response");
    }

    @Test
    void testProxyAuthenticationFailure() throws Exception {
        // Configure proxy settings with wrong password
        ProxySettings.saveSettings(
                false,
                "HTTP",
                "127.0.0.1",
                squidContainer.getMappedPort(3128),
                true,
                PROXY_USERNAME,
                "wrongpassword",
                "localhost",
                false
        );

        // Create HTTP client with proxy settings
        HttpClient client = createProxyClient();

        // Get proxy authorization header
        String proxyAuth = ProxySettings.getProxyAuthorizationHeader();

        // Make a request with wrong Proxy-Authorization header
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TEST_URL))
                .header("Proxy-Authorization", proxyAuth)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(407, response.statusCode(), "Should get 407 Proxy Authentication Required");
    }

    @Test
    void testMissingProxySettings() {
        // Configure proxy settings with empty server
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            ProxySettings.saveSettings(
                    false,
                    "HTTP",
                    "",
                    3128,
                    true,
                    PROXY_USERNAME,
                    PROXY_PASSWORD,
                    "localhost",
                    false
            );
        });

        assertTrue(exception.getMessage().contains("Proxy server cannot be empty"));
    }

    @Test
    void testMainApplicationProxyFlow() throws Exception {
        // Configure proxy settings
        ProxySettings.saveSettings(
                false,
                "HTTP",
                "127.0.0.1",
                squidContainer.getMappedPort(3128),
                true,
                PROXY_USERNAME,
                PROXY_PASSWORD,
                "localhost",
                false
        );

        // Set up authenticator directly
        java.net.Authenticator.setDefault(new java.net.Authenticator() {
            @Override
            protected java.net.PasswordAuthentication getPasswordAuthentication() {
                if (getRequestingHost().equalsIgnoreCase("127.0.0.1")) {
                    return new java.net.PasswordAuthentication(
                            PROXY_USERNAME,
                            PROXY_PASSWORD.toCharArray()
                    );
                }
                return null;
            }
        });

        // Create proxy configuration
        var proxy = ProxySettings.createProxy();
        var proxyAddress = (InetSocketAddress) proxy.address();
        var proxySelector = ProxySelector.of(proxyAddress);

        // Build HTTP client
        HttpClient client = HttpClient.newBuilder()
                .proxy(proxySelector)
                .version(HttpClient.Version.HTTP_2)
                .authenticator(java.net.Authenticator.getDefault())
                .build();

        // Create request with Proxy-Authorization header
        String auth = PROXY_USERNAME + ":" + PROXY_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://httpbin.org/status/200"))
                .header("Proxy-Authorization", authHeader)
                .GET()
                .build();

        // Send request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Should get 200 OK response");
    }

    @Test
    void testProxy() throws Exception {
        // Configure proxy settings
        ProxySettings.saveSettings(
                false,
                "HTTP",
                "127.0.0.1",
                squidContainer.getMappedPort(3128),
                true,
                PROXY_USERNAME,
                PROXY_PASSWORD,
                "localhost",
                false
        );

        // Create a custom authenticator that checks the requesting host
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() == RequestorType.PROXY) {
                    log.debug("Proxy authentication request - Host: {}, Scheme: {}, Protocol: {}, Type: {}",
                            getRequestingHost(), getRequestingScheme(), getRequestingProtocol(), getRequestorType());
                    return new PasswordAuthentication(PROXY_USERNAME, PROXY_PASSWORD.toCharArray());
                }
                log.debug("Not a proxy authentication request - Host: {}, Type: {}",
                        getRequestingHost(), getRequestorType());
                return null;
            }
        };
        Authenticator.setDefault(authenticator);

        // Create HTTP client with proxy settings
        var proxy = ProxySettings.createProxy();
        var proxyAddress = (InetSocketAddress) proxy.address();
        var proxySelector = ProxySelector.of(proxyAddress);

        // Create a trust manager that trusts all certificates for HTTPS
        TrustManager[] trustAllCerts = new TrustManager[]{
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

        // Create HTTP client with proxy settings
        HttpClient client = HttpClient.newBuilder()
                .proxy(proxySelector)
                .sslContext(sslContext)
                .authenticator(authenticator)
                .version(HttpClient.Version.HTTP_2)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TEST_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info(String.valueOf(response.headers()));
        assertEquals(200, response.statusCode(), "Should get 200 OK response");
    }
}
