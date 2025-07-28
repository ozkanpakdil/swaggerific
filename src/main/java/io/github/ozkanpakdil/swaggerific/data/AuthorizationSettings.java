package io.github.ozkanpakdil.swaggerific.data;

import io.github.ozkanpakdil.swaggerific.security.CredentialEncryption;
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

        // Encrypted versions of sensitive fields for serialization
        private String encryptedApiKeyValue;
        private String encryptedUsername;
        private String encryptedPassword;
        private String encryptedBearerToken;

        @Serial
        private void writeObject(ObjectOutputStream out) throws IOException {
            // Encrypt sensitive data before serialization
            this.encryptedApiKeyValue = CredentialEncryption.encrypt(apiKeyValue);
            this.encryptedUsername = CredentialEncryption.encrypt(username);
            this.encryptedPassword = CredentialEncryption.encrypt(password);
            this.encryptedBearerToken = CredentialEncryption.encrypt(bearerToken);

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

