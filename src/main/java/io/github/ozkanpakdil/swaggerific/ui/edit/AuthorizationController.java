package io.github.ozkanpakdil.swaggerific.ui.edit;

import io.github.ozkanpakdil.swaggerific.security.OAuth2Service;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the Authorization tab in the request panel.
 * Handles different authentication methods:
 * - No Auth
 * - API Key
 * - Basic Authentication
 * - Bearer Token
 */
public class AuthorizationController implements Initializable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthorizationController.class);

    public enum AuthType {
        NO_AUTH("No Auth"),
        API_KEY("API Key"),
        BASIC_AUTH("Basic Authentication"),
        BEARER_TOKEN("Bearer Token"),
        OAUTH2("OAuth 2.0");

        private final String displayName;

        AuthType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @FXML
    private ComboBox<AuthType> authTypeComboBox;

    @FXML
    private VBox apiKeyContainer;
    @FXML
    private TextField apiKeyField;
    @FXML
    private TextField apiKeyNameField;

    @FXML
    private VBox basicAuthContainer;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private VBox bearerTokenContainer;
    @FXML
    private TextField tokenField;
    
    // OAuth 2.0 UI components
    @FXML
    private VBox oauth2Container;
    @FXML
    private ComboBox<OAuth2Service.GrantType> grantTypeComboBox;
    @FXML
    private TextField clientIdField;
    @FXML
    private PasswordField clientSecretField;
    @FXML
    private TextField tokenUrlField;
    @FXML
    private TextField authUrlField;
    @FXML
    private TextField redirectUriField;
    @FXML
    private TextField scopeField;
    @FXML
    private TextField accessTokenField;
    @FXML
    private TextField refreshTokenField;

    private AuthType currentAuthType = AuthType.NO_AUTH;
    
    // OAuth 2.0 service
    private final OAuth2Service oauth2Service = new OAuth2Service();

    // Callback to be called when authorization settings change
    private Runnable onSettingsChangeCallback;

    /**
     * Sets a callback to be called when authorization settings change
     * 
     * @param callback the callback to call
     */
    public void setOnSettingsChangeCallback(Runnable callback) {
        this.onSettingsChangeCallback = callback;
    }

    /**
     * Notifies that authorization settings have changed
     */
    private void notifySettingsChanged() {
        if (onSettingsChangeCallback != null) {
            onSettingsChangeCallback.run();
        }
    }

    /**
     * Gets the API key name field
     */
    public TextField getApiKeyNameField() {
        return apiKeyNameField;
    }

    /**
     * Gets the API key field
     */
    public TextField getApiKeyField() {
        return apiKeyField;
    }

    /**
     * Gets the username field
     */
    public TextField getUsernameField() {
        return usernameField;
    }

    /**
     * Gets the password field
     */
    public PasswordField getPasswordField() {
        return passwordField;
    }

    /**
     * Gets the token field
     */
    public TextField getTokenField() {
        return tokenField;
    }
    
    /**
     * Gets the client ID field
     */
    public TextField getClientIdField() {
        return clientIdField;
    }
    
    /**
     * Gets the client secret field
     */
    public PasswordField getClientSecretField() {
        return clientSecretField;
    }
    
    /**
     * Gets the token URL field
     */
    public TextField getTokenUrlField() {
        return tokenUrlField;
    }
    
    /**
     * Gets the authorization URL field
     */
    public TextField getAuthUrlField() {
        return authUrlField;
    }
    
    /**
     * Gets the redirect URI field
     */
    public TextField getRedirectUriField() {
        return redirectUriField;
    }
    
    /**
     * Gets the scope field
     */
    public TextField getScopeField() {
        return scopeField;
    }
    
    /**
     * Gets the access token field
     */
    public TextField getAccessTokenField() {
        return accessTokenField;
    }
    
    /**
     * Gets the refresh token field
     */
    public TextField getRefreshTokenField() {
        return refreshTokenField;
    }
    
    /**
     * Gets the grant type combo box
     */
    public ComboBox<OAuth2Service.GrantType> getGrantTypeComboBox() {
        return grantTypeComboBox;
    }

    /**
     * Sets the current authentication type and updates the UI
     */
    public void setCurrentAuthType(AuthType authType) {
        if (authType != null) {
            authTypeComboBox.getSelectionModel().select(authType);
            updateAuthTypeVisibility(authType);
            currentAuthType = authType;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize the auth type combo box
        authTypeComboBox.setItems(FXCollections.observableArrayList(AuthType.values()));
        authTypeComboBox.getSelectionModel().select(AuthType.NO_AUTH);

        // Initialize the OAuth 2.0 grant type combo box
        grantTypeComboBox.setItems(FXCollections.observableArrayList(OAuth2Service.GrantType.values()));
        grantTypeComboBox.getSelectionModel().select(OAuth2Service.GrantType.AUTHORIZATION_CODE);

        // Add listener to show/hide appropriate fields based on selected auth type
        authTypeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateAuthTypeVisibility(newValue);
                currentAuthType = newValue;
                notifySettingsChanged();
            }
        });

        // Add listeners to text fields to notify when they change
        apiKeyNameField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        apiKeyField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        tokenField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        
        // Add listeners to OAuth 2.0 fields
        clientIdField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        clientSecretField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        tokenUrlField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        authUrlField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        redirectUriField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        scopeField.textProperty().addListener((observable, oldValue, newValue) -> notifySettingsChanged());
        
        // Add listener to grant type combo box
        grantTypeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateOAuth2FieldsVisibility(newValue);
                notifySettingsChanged();
            }
        });
        
        // Set default values for OAuth 2.0 fields
        redirectUriField.setText("http://localhost:8080/callback");
    }

    /**
     * Updates the visibility of authentication fields based on the selected auth type
     */
    private void updateAuthTypeVisibility(AuthType authType) {
        // Hide all containers first
        apiKeyContainer.setVisible(false);
        apiKeyContainer.setManaged(false);
        basicAuthContainer.setVisible(false);
        basicAuthContainer.setManaged(false);
        bearerTokenContainer.setVisible(false);
        bearerTokenContainer.setManaged(false);
        oauth2Container.setVisible(false);
        oauth2Container.setManaged(false);

        // Show the appropriate container based on auth type
        switch (authType) {
            case API_KEY:
                apiKeyContainer.setVisible(true);
                apiKeyContainer.setManaged(true);
                break;
            case BASIC_AUTH:
                basicAuthContainer.setVisible(true);
                basicAuthContainer.setManaged(true);
                break;
            case BEARER_TOKEN:
                bearerTokenContainer.setVisible(true);
                bearerTokenContainer.setManaged(true);
                break;
            case OAUTH2:
                oauth2Container.setVisible(true);
                oauth2Container.setManaged(true);
                // Update OAuth 2.0 fields visibility based on selected grant type
                updateOAuth2FieldsVisibility(grantTypeComboBox.getSelectionModel().getSelectedItem());
                break;
            case NO_AUTH:
            default:
                // No additional fields needed for NO_AUTH
                break;
        }
    }
    
    /**
     * Updates the visibility of OAuth 2.0 fields based on the selected grant type
     */
    private void updateOAuth2FieldsVisibility(OAuth2Service.GrantType grantType) {
        if (grantType == null) {
            return;
        }
        
        // Show/hide fields based on grant type
        switch (grantType) {
            case AUTHORIZATION_CODE:
                // Show all fields for Authorization Code flow
                authUrlField.setVisible(true);
                authUrlField.setManaged(true);
                redirectUriField.setVisible(true);
                redirectUriField.setManaged(true);
                break;
            case CLIENT_CREDENTIALS:
                // Hide authorization URL and redirect URI for Client Credentials flow
                authUrlField.setVisible(false);
                authUrlField.setManaged(false);
                redirectUriField.setVisible(false);
                redirectUriField.setManaged(false);
                break;
            case PASSWORD:
                // Hide authorization URL and redirect URI for Password flow
                authUrlField.setVisible(false);
                authUrlField.setManaged(false);
                redirectUriField.setVisible(false);
                redirectUriField.setManaged(false);
                break;
            case REFRESH_TOKEN:
                // Hide most fields for Refresh Token flow, only show token URL and refresh token
                authUrlField.setVisible(false);
                authUrlField.setManaged(false);
                redirectUriField.setVisible(false);
                redirectUriField.setManaged(false);
                break;
        }
    }

    /**
     * Gets the current authentication type
     */
    public AuthType getCurrentAuthType() {
        return currentAuthType;
    }

    /**
     * Applies the current authentication settings to the provided headers map
     * 
     * @param headers The headers map to update with authentication headers
     */
    public void applyAuthHeaders(Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<>();
        }

        switch (currentAuthType) {
            case API_KEY:
                if (apiKeyNameField.getText() != null && !apiKeyNameField.getText().isEmpty() &&
                    apiKeyField.getText() != null && !apiKeyField.getText().isEmpty()) {
                    headers.put(apiKeyNameField.getText(), apiKeyField.getText());
                    log.info("Applied API Key authentication with key name: {}", apiKeyNameField.getText());
                }
                break;
            case BASIC_AUTH:
                if (usernameField.getText() != null && !usernameField.getText().isEmpty() &&
                    passwordField.getText() != null) {
                    String auth = usernameField.getText() + ":" + passwordField.getText();
                    String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                    headers.put("Authorization", "Basic " + encodedAuth);
                    log.info("Applied Basic authentication for user: {}", usernameField.getText());
                }
                break;
            case BEARER_TOKEN:
                if (tokenField.getText() != null && !tokenField.getText().isEmpty()) {
                    headers.put("Authorization", "Bearer " + tokenField.getText());
                    log.info("Applied Bearer Token authentication");
                }
                break;
            case OAUTH2:
                // For OAuth 2.0, we need to get an access token first
                if (accessTokenField.getText() != null && !accessTokenField.getText().isEmpty()) {
                    // Use the existing access token if available
                    headers.put("Authorization", "Bearer " + accessTokenField.getText());
                    log.info("Applied OAuth 2.0 authentication with existing access token");
                } else {
                    // Get a new access token based on the grant type
                    OAuth2Service.GrantType grantType = grantTypeComboBox.getSelectionModel().getSelectedItem();
                    if (grantType != null && clientIdField.getText() != null && !clientIdField.getText().isEmpty() &&
                        clientSecretField.getText() != null && !clientSecretField.getText().isEmpty() &&
                        tokenUrlField.getText() != null && !tokenUrlField.getText().isEmpty()) {
                        
                        try {
                            // Use a final variable for the access token to use in lambda
                            final String[] accessTokenHolder = new String[1];
                            
                            switch (grantType) {
                                case CLIENT_CREDENTIALS:
                                    accessTokenHolder[0] = oauth2Service.getClientCredentialsToken(
                                            tokenUrlField.getText(),
                                            clientIdField.getText(),
                                            clientSecretField.getText(),
                                            scopeField.getText()
                                    ).get(); // Blocking call for simplicity
                                    break;
                                case PASSWORD:
                                    // For password grant, we use the username and password fields
                                    if (usernameField.getText() != null && !usernameField.getText().isEmpty() &&
                                        passwordField.getText() != null) {
                                        accessTokenHolder[0] = oauth2Service.getPasswordToken(
                                                tokenUrlField.getText(),
                                                clientIdField.getText(),
                                                clientSecretField.getText(),
                                                usernameField.getText(),
                                                passwordField.getText(),
                                                scopeField.getText()
                                        ).get(); // Blocking call for simplicity
                                    }
                                    break;
                                case REFRESH_TOKEN:
                                    // For refresh token grant, we use the refresh token field
                                    if (refreshTokenField.getText() != null && !refreshTokenField.getText().isEmpty()) {
                                        accessTokenHolder[0] = oauth2Service.refreshToken(
                                                tokenUrlField.getText(),
                                                clientIdField.getText(),
                                                clientSecretField.getText(),
                                                refreshTokenField.getText()
                                        ).get(); // Blocking call for simplicity
                                    }
                                    break;
                                case AUTHORIZATION_CODE:
                                    // Authorization Code flow requires user interaction and is handled separately
                                    log.warn("Authorization Code flow requires user interaction and cannot be applied automatically");
                                    break;
                            }
                            
                            if (accessTokenHolder[0] != null) {
                                headers.put("Authorization", "Bearer " + accessTokenHolder[0]);
                                
                                // Update the access token field
                                Platform.runLater(() -> accessTokenField.setText(accessTokenHolder[0]));
                                
                                log.info("Applied OAuth 2.0 authentication with new access token");
                            }
                        } catch (Exception e) {
                            log.error("Failed to get OAuth 2.0 access token", e);
                        }
                    }
                }
                break;
            case NO_AUTH:
            default:
                // No authentication headers needed
                break;
        }
    }
}
