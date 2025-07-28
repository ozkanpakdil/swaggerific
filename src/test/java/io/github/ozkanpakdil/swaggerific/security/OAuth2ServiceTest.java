package io.github.ozkanpakdil.swaggerific.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Tests for the OAuth2Service class.
 */
class OAuth2ServiceTest {
    private OAuth2Service oauth2Service;
    private ClientAndServer mockServer;
    
    @BeforeEach
    void setUp() {
        oauth2Service = new OAuth2Service();
        mockServer = ClientAndServer.startClientAndServer(8080);
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
        assertTrue(url.contains("client_id=" + clientId));
        assertTrue(url.contains("redirect_uri=" + redirectUri.replace(":", "%3A").replace("/", "%2F")));
        assertTrue(url.contains("scope=" + scope.replace(" ", "%20")));
        assertTrue(url.contains("state=" + state));
    }
    
    @Test
    void testClientCredentialsFlow() throws ExecutionException, InterruptedException {
        // Setup mock server to respond to token request
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withHeader("Authorization", "Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=") // test-client:test-secret
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"test-access-token\",\"token_type\":\"bearer\",\"expires_in\":3600}")
        );
        
        // Test client credentials flow
        CompletableFuture<String> tokenFuture = oauth2Service.getClientCredentialsToken(
            "http://localhost:8080/oauth2/token",
            "test-client",
            "test-secret",
            "read write"
        );
        
        String token = tokenFuture.get();
        assertEquals("test-access-token", token);
        
        // Verify the request was made correctly
        mockServer.verify(
            request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withHeader("Authorization", "Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=")
        );
    }
    
    @Test
    void testPasswordFlow() throws ExecutionException, InterruptedException {
        // Setup mock server to respond to token request
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withHeader("Authorization", "Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=") // test-client:test-secret
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"test-access-token\",\"token_type\":\"bearer\",\"expires_in\":3600,\"refresh_token\":\"test-refresh-token\"}")
        );
        
        // Test password flow
        CompletableFuture<String> tokenFuture = oauth2Service.getPasswordToken(
            "http://localhost:8080/oauth2/token",
            "test-client",
            "test-secret",
            "testuser",
            "testpassword",
            "read write"
        );
        
        String token = tokenFuture.get();
        assertEquals("test-access-token", token);
        
        // Verify the request was made correctly
        mockServer.verify(
            request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withHeader("Authorization", "Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=")
        );
    }
    
    @Test
    void testRefreshToken() throws ExecutionException, InterruptedException {
        // Setup mock server to respond to token request
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"new-access-token\",\"token_type\":\"bearer\",\"expires_in\":3600,\"refresh_token\":\"new-refresh-token\"}")
        );
        
        // Test refresh token flow
        CompletableFuture<String> tokenFuture = oauth2Service.refreshToken(
            "http://localhost:8080/oauth2/token",
            "test-client",
            "test-secret",
            "test-refresh-token"
        );
        
        String token = tokenFuture.get();
        assertEquals("new-access-token", token);
        
        // Verify the request was made correctly
        mockServer.verify(
            request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
        );
    }
    
    @Test
    void testExchangeCodeForToken() throws ExecutionException, InterruptedException {
        // Setup mock server to respond to token request
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"test-access-token\",\"token_type\":\"bearer\",\"expires_in\":3600,\"refresh_token\":\"test-refresh-token\"}")
        );
        
        // Test authorization code flow
        CompletableFuture<String> tokenFuture = oauth2Service.exchangeCodeForToken(
            "http://localhost:8080/oauth2/token",
            "test-client",
            "test-secret",
            "test-code",
            "http://localhost:8080/callback"
        );
        
        String token = tokenFuture.get();
        assertEquals("test-access-token", token);
        
        // Verify the request was made correctly
        mockServer.verify(
            request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
        );
    }
}