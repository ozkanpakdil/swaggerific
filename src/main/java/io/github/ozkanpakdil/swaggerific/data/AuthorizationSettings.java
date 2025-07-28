package io.github.ozkanpakdil.swaggerific.data;

import io.github.ozkanpakdil.swaggerific.security.CredentialEncryption;
import io.github.ozkanpakdil.swaggerific.security.OAuth2Service;
import io.github.ozkanpakdil.swaggerific.ui.edit.AuthorizationController;
import io.github.ozkanpakdil.swaggerific.ui.edit.AuthorizationController.AuthType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for storing authorization settings by URL.
 * This class is serializable and can be saved to a file.
 */
public class AuthorizationSettings implements Serializable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthorizationSettings.class);
    
    @Serial
    private static final long serialVersionUID = -2338626292552177485L;

    // Map of URL to authorization settings
    private Map<String, AuthSetting> settingsByUrl = new HashMap<>();
    
    /**
     * Inner class to store authorization settings for a specific URL.
     */
    public static class AuthSetting implements Serializable {
        @Serial
        private static final long serialVersionUID = -1234567890123456789L;

        private AuthType authType;
        private transient String apiKeyName;
        private transient String apiKeyValue;
        private transient String username;
        private transient String password;
        private transient String bearerToken;
        
        // OAuth 2.0 specific fields
        private transient String clientId;
        private transient String clientSecret;
        private transient String tokenUrl;
        private transient String authorizationUrl;
        private transient String redirectUri;
        private transient String scope;
        private transient String accessToken;
        private transient String refreshToken;

        // Encrypted versions of sensitive fields for serialization
        private String encryptedApiKeyValue;
        private String encryptedUsername;
        private String encryptedPassword;
        private String encryptedBearerToken;
        private String encryptedClientId;
        private String encryptedClientSecret;
        private String encryptedTokenUrl;
        private String encryptedAuthorizationUrl;
        private String encryptedRedirectUri;
        private String encryptedScope;
        private String encryptedAccessToken;
        private String encryptedRefreshToken;

        @Serial
        private void writeObject(ObjectOutputStream out) throws IOException {
            // Encrypt sensitive data before serialization
            this.encryptedApiKeyValue = CredentialEncryption.encrypt(apiKeyValue);
            this.encryptedUsername = CredentialEncryption.encrypt(username);
            this.encryptedPassword = CredentialEncryption.encrypt(password);
            this.encryptedBearerToken = CredentialEncryption.encrypt(bearerToken);
            
            // Encrypt OAuth 2.0 sensitive data
            this.encryptedClientId = CredentialEncryption.encrypt(clientId);
            this.encryptedClientSecret = CredentialEncryption.encrypt(clientSecret);
            this.encryptedTokenUrl = CredentialEncryption.encrypt(tokenUrl);
            this.encryptedAuthorizationUrl = CredentialEncryption.encrypt(authorizationUrl);
            this.encryptedRedirectUri = CredentialEncryption.encrypt(redirectUri);
            this.encryptedScope = CredentialEncryption.encrypt(scope);
            this.encryptedAccessToken = CredentialEncryption.encrypt(accessToken);
            this.encryptedRefreshToken = CredentialEncryption.encrypt(refreshToken);

            out.defaultWriteObject();
            out.writeObject(apiKeyName); // apiKeyName is not sensitive, but still needs manual writing due to transient
        }

        @Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();

            // Read non-sensitive transient field
            this.apiKeyName = (String) in.readObject();

            // Decrypt sensitive data after deserialization
            this.apiKeyValue = CredentialEncryption.decrypt(encryptedApiKeyValue);
            this.username = CredentialEncryption.decrypt(encryptedUsername);
            this.password = CredentialEncryption.decrypt(encryptedPassword);
            this.bearerToken = CredentialEncryption.decrypt(encryptedBearerToken);
            
            // Decrypt OAuth 2.0 sensitive data
            this.clientId = CredentialEncryption.decrypt(encryptedClientId);
            this.clientSecret = CredentialEncryption.decrypt(encryptedClientSecret);
            this.tokenUrl = CredentialEncryption.decrypt(encryptedTokenUrl);
            this.authorizationUrl = CredentialEncryption.decrypt(encryptedAuthorizationUrl);
            this.redirectUri = CredentialEncryption.decrypt(encryptedRedirectUri);
            this.scope = CredentialEncryption.decrypt(encryptedScope);
            this.accessToken = CredentialEncryption.decrypt(encryptedAccessToken);
            this.refreshToken = CredentialEncryption.decrypt(encryptedRefreshToken);
        }

        public AuthSetting() {
            this.authType = AuthType.NO_AUTH;
        }
        
        public AuthSetting(AuthType authType) {
            this.authType = authType;
        }
        
        public AuthType getAuthType() {
            return authType;
        }
        
        public void setAuthType(AuthType authType) {
            this.authType = authType;
        }
        
        public String getApiKeyName() {
            return apiKeyName;
        }
        
        public void setApiKeyName(String apiKeyName) {
            this.apiKeyName = apiKeyName;
        }
        
        public String getApiKeyValue() {
            return apiKeyValue;
        }
        
        public void setApiKeyValue(String apiKeyValue) {
            this.apiKeyValue = apiKeyValue;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getBearerToken() {
            return bearerToken;
        }
        
        public void setBearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
        }
        
        // OAuth 2.0 getters and setters
        
        public String getClientId() {
            return clientId;
        }
        
        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
        
        public String getClientSecret() {
            return clientSecret;
        }
        
        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
        
        public String getTokenUrl() {
            return tokenUrl;
        }
        
        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }
        
        public String getAuthorizationUrl() {
            return authorizationUrl;
        }
        
        public void setAuthorizationUrl(String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
        }
        
        public String getRedirectUri() {
            return redirectUri;
        }
        
        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
        
        public String getScope() {
            return scope;
        }
        
        public void setScope(String scope) {
            this.scope = scope;
        }
        
        public String getAccessToken() {
            return accessToken;
        }
        
        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
        
        public String getRefreshToken() {
            return refreshToken;
        }
        
        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
    
    /**
     * Gets the authorization settings for a specific URL.
     * 
     * @param url the URL
     * @return the authorization settings, or null if not found
     */
    public AuthSetting getSettingsForUrl(String url) {
        return settingsByUrl.get(url);
    }
    
    /**
     * Saves the authorization settings for a specific URL.
     * 
     * @param url the URL
     * @param authController the authorization controller
     */
    public void saveSettingsForUrl(String url, AuthorizationController authController) {
        if (url == null || url.isEmpty() || authController == null) {
            return;
        }
        
        AuthSetting setting = new AuthSetting(authController.getCurrentAuthType());
        
        // Save settings based on auth type
        switch (authController.getCurrentAuthType()) {
            case API_KEY:
                setting.setApiKeyName(authController.getApiKeyNameField().getText());
                setting.setApiKeyValue(authController.getApiKeyField().getText());
                break;
            case BASIC_AUTH:
                setting.setUsername(authController.getUsernameField().getText());
                setting.setPassword(authController.getPasswordField().getText());
                break;
            case BEARER_TOKEN:
                setting.setBearerToken(authController.getTokenField().getText());
                break;
            case OAUTH2:
                // Save OAuth 2.0 settings
                setting.setClientId(authController.getClientIdField().getText());
                setting.setClientSecret(authController.getClientSecretField().getText());
                setting.setTokenUrl(authController.getTokenUrlField().getText());
                setting.setAuthorizationUrl(authController.getAuthUrlField().getText());
                setting.setRedirectUri(authController.getRedirectUriField().getText());
                setting.setScope(authController.getScopeField().getText());
                setting.setAccessToken(authController.getAccessTokenField().getText());
                setting.setRefreshToken(authController.getRefreshTokenField().getText());
                break;
            case NO_AUTH:
            default:
                // No additional settings needed
                break;
        }
        
        settingsByUrl.put(url, setting);
        log.debug("Saved authorization settings for URL: {}", url);
    }
    
    /**
     * Applies the saved authorization settings to an authorization controller.
     * 
     * @param url the URL
     * @param authController the authorization controller
     * @return true if settings were applied, false otherwise
     */
    public boolean applySettingsToController(String url, AuthorizationController authController) {
        if (url == null || url.isEmpty() || authController == null) {
            return false;
        }
        
        AuthSetting setting = settingsByUrl.get(url);
        if (setting == null) {
            return false;
        }
        
        // Set auth type first
        authController.setCurrentAuthType(setting.getAuthType());
        
        // Apply settings based on auth type
        switch (setting.getAuthType()) {
            case API_KEY:
                authController.getApiKeyNameField().setText(setting.getApiKeyName());
                authController.getApiKeyField().setText(setting.getApiKeyValue());
                break;
            case BASIC_AUTH:
                authController.getUsernameField().setText(setting.getUsername());
                authController.getPasswordField().setText(setting.getPassword());
                break;
            case BEARER_TOKEN:
                authController.getTokenField().setText(setting.getBearerToken());
                break;
            case OAUTH2:
                // Apply OAuth 2.0 settings
                authController.getClientIdField().setText(setting.getClientId());
                authController.getClientSecretField().setText(setting.getClientSecret());
                authController.getTokenUrlField().setText(setting.getTokenUrl());
                authController.getAuthUrlField().setText(setting.getAuthorizationUrl());
                authController.getRedirectUriField().setText(setting.getRedirectUri());
                authController.getScopeField().setText(setting.getScope());
                authController.getAccessTokenField().setText(setting.getAccessToken());
                authController.getRefreshTokenField().setText(setting.getRefreshToken());
                
                // Set the grant type if it was saved
                if (setting.getClientId() != null && !setting.getClientId().isEmpty()) {
                    // Default to CLIENT_CREDENTIALS if we have a client ID but no specific grant type saved
                    authController.getGrantTypeComboBox().getSelectionModel().select(OAuth2Service.GrantType.CLIENT_CREDENTIALS);
                }
                break;
            case NO_AUTH:
            default:
                // No additional settings needed
                break;
        }
        
        log.info("Applied authorization settings for URL: {}", url);
        return true;
    }
    
    /**
     * Clears all authorization settings.
     */
    public void clear() {
        settingsByUrl.clear();
    }
    
    /**
     * Gets the number of saved authorization settings.
     * 
     * @return the number of saved authorization settings
     */
    public int size() {
        return settingsByUrl.size();
    }
    
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}

