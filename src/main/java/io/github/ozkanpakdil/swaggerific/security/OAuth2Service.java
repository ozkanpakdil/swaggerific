package io.github.ozkanpakdil.swaggerific.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for handling OAuth 2.0 authentication flows.
 * Supports Authorization Code Flow and Client Credentials Flow.
 */
public class OAuth2Service {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OAuth2Service.class);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // OAuth 2.0 token cache
    private static class TokenCache {
        String accessToken;
        String refreshToken;
        Instant expiresAt;
        
        boolean isValid() {
            return accessToken != null && expiresAt != null && Instant.now().isBefore(expiresAt);
        }
    }
    
    private final Map<String, TokenCache> tokenCache = new HashMap<>();
    
    public OAuth2Service() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * OAuth 2.0 grant types
     */
    public enum GrantType {
        AUTHORIZATION_CODE("authorization_code"),
        CLIENT_CREDENTIALS("client_credentials"),
        PASSWORD("password"),
        REFRESH_TOKEN("refresh_token");
        
        private final String value;
        
        GrantType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Get an access token using the client credentials flow.
     * 
     * @param tokenUrl the token endpoint URL
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param scope the requested scope (optional)
     * @return a CompletableFuture that will complete with the access token
     */
    public CompletableFuture<String> getClientCredentialsToken(String tokenUrl, String clientId, String clientSecret, String scope) {
        // Check cache first
        String cacheKey = tokenUrl + "|" + clientId + "|" + scope;
        TokenCache cachedToken = tokenCache.get(cacheKey);
        if (cachedToken != null && cachedToken.isValid()) {
            log.debug("Using cached OAuth 2.0 token for {}", clientId);
            return CompletableFuture.completedFuture(cachedToken.accessToken);
        }
        
        // Build request body
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", GrantType.CLIENT_CREDENTIALS.getValue());
        if (scope != null && !scope.isEmpty()) {
            formData.put("scope", scope);
        }
        
        String encodedFormData = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + 
                     URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        
        // Create authorization header with Basic auth
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        // Build request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + encodedAuth)
                .POST(HttpRequest.BodyPublishers.ofString(encodedFormData))
                .build();
        
        // Send request and process response
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        log.error("OAuth 2.0 token request failed: {} {}", response.statusCode(), response.body());
                        throw new RuntimeException("OAuth 2.0 token request failed: " + response.statusCode());
                    }
                    return response.body();
                })
                .thenApply(body -> {
                    try {
                        JsonNode node = objectMapper.readTree(body);
                        String accessToken = node.get("access_token").asText();
                        
                        // Cache the token
                        TokenCache tokenCache = new TokenCache();
                        tokenCache.accessToken = accessToken;
                        
                        // Set expiration time if provided
                        if (node.has("expires_in")) {
                            int expiresIn = node.get("expires_in").asInt();
                            tokenCache.expiresAt = Instant.now().plusSeconds(expiresIn);
                        } else {
                            // Default to 1 hour if not provided
                            tokenCache.expiresAt = Instant.now().plusSeconds(3600);
                        }
                        
                        // Store refresh token if provided
                        if (node.has("refresh_token")) {
                            tokenCache.refreshToken = node.get("refresh_token").asText();
                        }
                        
                        this.tokenCache.put(cacheKey, tokenCache);
                        log.info("Obtained OAuth 2.0 token for {}", clientId);
                        return accessToken;
                    } catch (IOException e) {
                        log.error("Failed to parse OAuth 2.0 token response", e);
                        throw new RuntimeException("Failed to parse OAuth 2.0 token response", e);
                    }
                });
    }
    
    /**
     * Get the authorization URL for the authorization code flow.
     * 
     * @param authorizationUrl the authorization endpoint URL
     * @param clientId the client ID
     * @param redirectUri the redirect URI
     * @param scope the requested scope (optional)
     * @param state a random state value for CSRF protection
     * @return the authorization URL
     */
    public String getAuthorizationUrl(String authorizationUrl, String clientId, String redirectUri, String scope, String state) {
        StringBuilder url = new StringBuilder(authorizationUrl);
        url.append("?response_type=code");
        url.append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        
        if (scope != null && !scope.isEmpty()) {
            url.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
        }
        
        if (state != null && !state.isEmpty()) {
            url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        
        return url.toString();
    }
    
    /**
     * Exchange an authorization code for an access token.
     * 
     * @param tokenUrl the token endpoint URL
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param code the authorization code
     * @param redirectUri the redirect URI
     * @return a CompletableFuture that will complete with the access token
     */
    public CompletableFuture<String> exchangeCodeForToken(String tokenUrl, String clientId, String clientSecret, 
                                                         String code, String redirectUri) {
        // Build request body
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", GrantType.AUTHORIZATION_CODE.getValue());
        formData.put("code", code);
        formData.put("redirect_uri", redirectUri);
        formData.put("client_id", clientId);
        formData.put("client_secret", clientSecret);
        
        String encodedFormData = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + 
                     URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        
        // Build request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodedFormData))
                .build();
        
        // Send request and process response
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        log.error("OAuth 2.0 token request failed: {} {}", response.statusCode(), response.body());
                        throw new RuntimeException("OAuth 2.0 token request failed: " + response.statusCode());
                    }
                    return response.body();
                })
                .thenApply(body -> {
                    try {
                        JsonNode node = objectMapper.readTree(body);
                        String accessToken = node.get("access_token").asText();
                        
                        // Cache the token
                        String cacheKey = tokenUrl + "|" + clientId;
                        TokenCache tokenCache = new TokenCache();
                        tokenCache.accessToken = accessToken;
                        
                        // Set expiration time if provided
                        if (node.has("expires_in")) {
                            int expiresIn = node.get("expires_in").asInt();
                            tokenCache.expiresAt = Instant.now().plusSeconds(expiresIn);
                        } else {
                            // Default to 1 hour if not provided
                            tokenCache.expiresAt = Instant.now().plusSeconds(3600);
                        }
                        
                        // Store refresh token if provided
                        if (node.has("refresh_token")) {
                            tokenCache.refreshToken = node.get("refresh_token").asText();
                        }
                        
                        this.tokenCache.put(cacheKey, tokenCache);
                        log.info("Obtained OAuth 2.0 token for {}", clientId);
                        return accessToken;
                    } catch (IOException e) {
                        log.error("Failed to parse OAuth 2.0 token response", e);
                        throw new RuntimeException("Failed to parse OAuth 2.0 token response", e);
                    }
                });
    }
    
    /**
     * Refresh an access token using a refresh token.
     * 
     * @param tokenUrl the token endpoint URL
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param refreshToken the refresh token
     * @return a CompletableFuture that will complete with the new access token
     */
    public CompletableFuture<String> refreshToken(String tokenUrl, String clientId, String clientSecret, String refreshToken) {
        // Build request body
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", GrantType.REFRESH_TOKEN.getValue());
        formData.put("refresh_token", refreshToken);
        formData.put("client_id", clientId);
        formData.put("client_secret", clientSecret);
        
        String encodedFormData = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + 
                     URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        
        // Build request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodedFormData))
                .build();
        
        // Send request and process response
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        log.error("OAuth 2.0 token refresh failed: {} {}", response.statusCode(), response.body());
                        throw new RuntimeException("OAuth 2.0 token refresh failed: " + response.statusCode());
                    }
                    return response.body();
                })
                .thenApply(body -> {
                    try {
                        JsonNode node = objectMapper.readTree(body);
                        String accessToken = node.get("access_token").asText();
                        
                        // Update cache
                        String cacheKey = tokenUrl + "|" + clientId;
                        TokenCache tokenCache = this.tokenCache.getOrDefault(cacheKey, new TokenCache());
                        tokenCache.accessToken = accessToken;
                        
                        // Set expiration time if provided
                        if (node.has("expires_in")) {
                            int expiresIn = node.get("expires_in").asInt();
                            tokenCache.expiresAt = Instant.now().plusSeconds(expiresIn);
                        } else {
                            // Default to 1 hour if not provided
                            tokenCache.expiresAt = Instant.now().plusSeconds(3600);
                        }
                        
                        // Update refresh token if provided
                        if (node.has("refresh_token")) {
                            tokenCache.refreshToken = node.get("refresh_token").asText();
                        }
                        
                        this.tokenCache.put(cacheKey, tokenCache);
                        log.info("Refreshed OAuth 2.0 token for {}", clientId);
                        return accessToken;
                    } catch (IOException e) {
                        log.error("Failed to parse OAuth 2.0 token response", e);
                        throw new RuntimeException("Failed to parse OAuth 2.0 token response", e);
                    }
                });
    }
    
    /**
     * Get an access token using the password grant type.
     * 
     * @param tokenUrl the token endpoint URL
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param username the username
     * @param password the password
     * @param scope the requested scope (optional)
     * @return a CompletableFuture that will complete with the access token
     */
    public CompletableFuture<String> getPasswordToken(String tokenUrl, String clientId, String clientSecret, 
                                                     String username, String password, String scope) {
        // Build request body
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", GrantType.PASSWORD.getValue());
        formData.put("username", username);
        formData.put("password", password);
        
        if (scope != null && !scope.isEmpty()) {
            formData.put("scope", scope);
        }
        
        String encodedFormData = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + 
                     URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        
        // Create authorization header with Basic auth
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        // Build request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + encodedAuth)
                .POST(HttpRequest.BodyPublishers.ofString(encodedFormData))
                .build();
        
        // Send request and process response
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        log.error("OAuth 2.0 token request failed: {} {}", response.statusCode(), response.body());
                        throw new RuntimeException("OAuth 2.0 token request failed: " + response.statusCode());
                    }
                    return response.body();
                })
                .thenApply(body -> {
                    try {
                        JsonNode node = objectMapper.readTree(body);
                        String accessToken = node.get("access_token").asText();
                        
                        // Cache the token
                        String cacheKey = tokenUrl + "|" + clientId + "|" + username;
                        TokenCache tokenCache = new TokenCache();
                        tokenCache.accessToken = accessToken;
                        
                        // Set expiration time if provided
                        if (node.has("expires_in")) {
                            int expiresIn = node.get("expires_in").asInt();
                            tokenCache.expiresAt = Instant.now().plusSeconds(expiresIn);
                        } else {
                            // Default to 1 hour if not provided
                            tokenCache.expiresAt = Instant.now().plusSeconds(3600);
                        }
                        
                        // Store refresh token if provided
                        if (node.has("refresh_token")) {
                            tokenCache.refreshToken = node.get("refresh_token").asText();
                        }
                        
                        this.tokenCache.put(cacheKey, tokenCache);
                        log.info("Obtained OAuth 2.0 token for {}", username);
                        return accessToken;
                    } catch (IOException e) {
                        log.error("Failed to parse OAuth 2.0 token response", e);
                        throw new RuntimeException("Failed to parse OAuth 2.0 token response", e);
                    }
                });
    }
}