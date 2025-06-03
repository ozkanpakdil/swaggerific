package io.github.ozkanpakdil.swaggerific.ui.edit;

import atlantafx.base.controls.ToggleSwitch;
import io.github.ozkanpakdil.swaggerific.tools.ProxySettings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Proxy {
    private static final Logger log = LoggerFactory.getLogger(Proxy.class);

    @FXML
    private CheckBox proxyRequiresAuth;
    @FXML
    private TextField proxyUsername;
    @FXML
    private PasswordField proxyPassword;
    @FXML
    private ComboBox<String> proxyType;
    @FXML
    private CheckBox useSystemProxy;
    @FXML
    private TextField proxyServer;
    @FXML
    private TextField proxyPort;
    @FXML
    private ToggleSwitch proxyAuth;
    @FXML
    private TextField proxyAuthUsername;
    @FXML
    private PasswordField proxyAuthPassword;
    @FXML
    private TextArea proxyBypass;
    @FXML
    private CheckBox disableSslValidation;

    @FXML
    public void initialize() {
        // Bind visibility for system proxy auth fields
        proxyUsername.managedProperty().bind(proxyRequiresAuth.selectedProperty());
        proxyPassword.managedProperty().bind(proxyRequiresAuth.selectedProperty());

        // Bind visibility for custom proxy auth fields
        proxyAuthUsername.managedProperty().bind(proxyAuth.selectedProperty());
        proxyAuthPassword.managedProperty().bind(proxyAuth.selectedProperty());

        // Load saved settings
        loadProxySettings();
    }

    private void loadProxySettings() {
        // Load settings from preferences
        useSystemProxy.setSelected(ProxySettings.useSystemProxy());
        proxyType.setValue(ProxySettings.getProxyType());
        proxyServer.setText(ProxySettings.getProxyServer());
        proxyPort.setText(String.valueOf(ProxySettings.getProxyPort()));
        proxyAuth.setSelected(ProxySettings.useProxyAuth());
        proxyAuthUsername.setText(ProxySettings.getProxyAuthUsername());

        // Securely handle password
        char[] passwordChars = ProxySettings.getProxyAuthPassword();
        if (passwordChars.length > 0) {
            proxyAuthPassword.setText(new String(passwordChars));
            // Clear the password from memory
            Arrays.fill(passwordChars, '\0');
        } else {
            proxyAuthPassword.setText("");
        }

        proxyBypass.setText(String.join(",", ProxySettings.getProxyBypass()));

        // Load SSL validation setting
        disableSslValidation.setSelected(ProxySettings.disableSslValidation());
    }

    /**
     * Saves proxy settings when the save button is clicked. This method securely handles proxy credentials and ensures they are
     * properly saved.
     *
     * @param actionEvent The action event
     */
    public void saveProxySettings(ActionEvent actionEvent) {
        try {
            int port;
            try {
                port = Integer.parseInt(proxyPort.getText());
            } catch (NumberFormatException e) {
                log.error("Port must be an integer (1-65535)");
                return; // abort save, keep dialog open
            }

            // Get password from UI
            String password = proxyAuthPassword.getText();

            // Save settings to preferences
            ProxySettings.saveSettings(
                    useSystemProxy.isSelected(),
                    proxyType.getValue() != null ? proxyType.getValue() : "HTTP",
                    proxyServer.getText(),
                    port,
                    proxyAuth.isSelected(),
                    proxyAuthUsername.getText(),
                    password,
                    proxyBypass.getText(),
                    disableSslValidation.isSelected()
            );

            // Clear password field for security
            proxyAuthPassword.clear();

            // Reinitialize proxy settings to apply changes immediately
            io.github.ozkanpakdil.swaggerific.SwaggerApplication.initializeProxySettings();

            log.info("Proxy settings saved and applied successfully");
        } catch (Exception e) {
            // Log error without including any sensitive information
            log.error("Error saving proxy settings: {}", e.getMessage());
        }
    }
}
