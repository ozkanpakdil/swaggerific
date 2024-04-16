package com.mascix.swaggerific.ui.edit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ComboBox;

public class Proxy {
    @FXML
    private CheckBox proxyRequiresAuth;
    @FXML
    private TextField proxyUsername;
    @FXML
    private PasswordField proxyPassword;
    @FXML
    private ComboBox<String> proxyType;

    @FXML
    public void initialize() {
        proxyUsername.managedProperty().bind(proxyRequiresAuth.selectedProperty());
        proxyPassword.managedProperty().bind(proxyRequiresAuth.selectedProperty());
    }

    public void saveProxySettings(ActionEvent actionEvent) {
        // Save settings
    }
}