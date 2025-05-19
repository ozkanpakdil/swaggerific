package io.github.ozkanpakdil.swaggerific.ui.edit;

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
        BEARER_TOKEN("Bearer Token");

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

    private AuthType currentAuthType = AuthType.NO_AUTH;

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
            case NO_AUTH:
            default:
                // No additional fields needed for NO_AUTH
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
            case NO_AUTH:
            default:
                // No authentication headers needed
                break;
        }
    }
}
