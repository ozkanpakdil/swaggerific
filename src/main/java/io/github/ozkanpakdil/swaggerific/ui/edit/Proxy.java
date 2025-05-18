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
        proxyAuthPassword.setText(ProxySettings.getProxyAuthPassword());
        proxyBypass.setText(String.join(",", ProxySettings.getProxyBypass()));

        // TODO: Load system proxy auth settings if available
    }

    public void saveProxySettings(ActionEvent actionEvent) {
        try {
            // Parse port number
            int port = 8080;
            try {
                port = Integer.parseInt(proxyPort.getText());
            } catch (NumberFormatException e) {
                log.warn("Invalid port number, using default: 8080");
            }

            // Save settings to preferences
            ProxySettings.saveSettings(
                useSystemProxy.isSelected(),
                proxyType.getValue(),
                proxyServer.getText(),
                port,
                proxyAuth.isSelected(),
                proxyAuthUsername.getText(),
                proxyAuthPassword.getText(),
                proxyBypass.getText()
            );

            // Apply proxy settings immediately
            ProxySettings.setupProxyAuthentication();

            log.info("Proxy settings saved successfully");
        } catch (Exception e) {
            log.error("Error saving proxy settings", e);
        }
    }
}
