package io.github.ozkanpakdil.swaggerific.ui.edit;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class Addons implements Initializable {
    public static final String KEY_ADDONS_ENABLED = "ui.addons.enabled";

    @FXML private CheckBox chkEnableAddons;

    private final Preferences prefs = Preferences.userNodeForPackage(io.github.ozkanpakdil.swaggerific.SwaggerApplication.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chkEnableAddons.setSelected(prefs.getBoolean(KEY_ADDONS_ENABLED, false));
    }

    @FXML
    public void onEnableChanged() {
        prefs.putBoolean(KEY_ADDONS_ENABLED, chkEnableAddons.isSelected());
    }
}
