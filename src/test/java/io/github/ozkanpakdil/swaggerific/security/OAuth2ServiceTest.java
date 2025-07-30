package io.github.ozkanpakdil.swaggerific.security;

import io.github.ozkanpakdil.swaggerific.SimpleHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the OAuth2Service class.
 */
class OAuth2ServiceTest {
    private OAuth2Service oauth2Service;
    private SimpleHttpServer httpServer;
    private static final int PORT = 8080;
    
    @BeforeEach
    void setUp() throws IOException {
        oauth2Service = new OAuth2Service();
        httpServer = new SimpleHttpServer(PORT);
        httpServer.start();
    }
    
    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }
    
    @Test
    void testGetAuthorizationUrl() {
        String authUrl = "https://auth.example.com/oauth2/authorize";
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";
        String scope = "read write";
        String state = "random-state";
        
        String url = oauth2Service.getAuthorizationUrl(authUrl, clientId, redirectUri, scope, state);
        
        assertTrue(url.startsWith(authUrl));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)));
        assertTrue(url.contains("redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)));
        assertTrue(url.contains("scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)));
        assertTrue(url.contains("state=" + URLEncoder.encode(state, StandardCharsets.UTF_8)));
    }
    
    @Test
    void testClientCredentialsFlow() throws ExecutionException, InterruptedException {
        // Setup server to respond to token request
        String responseBody = "{\"access_token\":\"test-access-token\",\"token_type\":\"bearer\",\"expires_in\":3600}";
        httpServer.addResponse("/oauth2/token", responseBody, "application/json", 200);
        
        // Test client credentials flow
        CompletableFuture<String> tokenFuture = oauth2Service.getClientCredentialsToken(
            "http://localhost:" + PORT + "/oauth2/token",
            "test-client",
            "test-secret",
            "read write"
        );
        
        String token = tokenFuture.get();
        assertEquals("test-access-token", token);
    }
    
    @Test
    void testPasswordFlow() throws ExecutionException, InterruptedException {
        // Setup server to respond to token request
        String responseBody = "{\"access_token\":\"test-access-token\",\"token_type\":\"bearer\",\"expires_in\":3600,\"refresh_token\":\"test-refresh-token\"}";
        httpServer.addResponse("/oauth2/token", responseBody, "application/json", 200);
        
        // Test password flow
        CompletableFuture<String> tokenFuture = oauth2Service.getPasswordToken(
            "http://localhost:" + PORT + "/oauth2/token",
            "test-client",
            "test-secret",
            "testuser",
            "testpassword",
            "read write"
        );
        
        String token = tokenFuture.get();
        assertEquals("test-access-token", token);
    }
    
    @Test
    void testRefreshToken() throws ExecutionException, InterruptedException {
        // Setup server to respond to token request
        String responseBody = "{\"access_token\":\"new-access-token\",\"token_type\":\"bearer\",\"expires_in\":3600,\"refresh_token\":\"new-refresh-token\"}";
        httpServer.addResponse("/oauth2/token", responseBody, "application/json", 200);
        
        // Test refresh token flow
        CompletableFuture<String> tokenFuture = oauth2Service.refreshToken(
            "http://localhost:" + PORT + "/oauth2/token",
            "test-client",
            "test-secret",
            "test-refresh-token"
        );
        
        String token = tokenFuture.get();
        assertEquals("new-access-token", token);
    }
    
    @Test
    void testExchangeCodeForToken() throws ExecutionException, InterruptedException {
        // Setup server to respond to token request
        String responseBody = "{\"access_token\":\"test-access-token\",\"token_type\":\"bearer\",\"expires_in\":3600,\"refresh_token\":\"test-refresh-token\"}";
        httpServer.addResponse("/oauth2/token", responseBody, "application/json", 200);
        
        // Test authorization code flow
        CompletableFuture<String> tokenFuture = oauth2Service.exchangeCodeForToken(
            "http://localhost:" + PORT + "/oauth2/token",
            "test-client",
            "test-secret",
            "test-code",
            "http://localhost:" + PORT + "/callback"
        );
        
        String token = tokenFuture.get();
        assertEquals("test-access-token", token);
    }
}