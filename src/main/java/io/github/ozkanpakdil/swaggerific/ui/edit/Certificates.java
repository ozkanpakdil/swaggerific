package io.github.ozkanpakdil.swaggerific.ui.edit;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class Certificates implements Initializable {
    public static final String KEY_CA_BUNDLE_ENABLED = "certs.caBundleEnabled";
    public static final String KEY_CA_BUNDLE_PATH = "certs.caBundlePath";

    @FXML private CheckBox chkEnableCaBundle;
    @FXML private TextField txtCaBundlePath;

    private final Preferences prefs = Preferences.userNodeForPackage(io.github.ozkanpakdil.swaggerific.SwaggerApplication.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chkEnableCaBundle.setSelected(prefs.getBoolean(KEY_CA_BUNDLE_ENABLED, false));
        // Persist changes whenever selection toggles (more reliable than onAction timing)
        chkEnableCaBundle.selectedProperty().addListener((obs, oldV, newV) -> {
                prefs.putBoolean(KEY_CA_BUNDLE_ENABLED, newV);
                try { prefs.flush(); } catch (Exception ignored) {}
        });
        txtCaBundlePath.setText(prefs.get(KEY_CA_BUNDLE_PATH, ""));
    }

    @FXML
    public void onEnableChanged() {
        prefs.putBoolean(KEY_CA_BUNDLE_ENABLED, chkEnableCaBundle.isSelected());
    }

    @FXML
    public void onPathChanged() {
        String path = txtCaBundlePath.getText() == null ? "" : txtCaBundlePath.getText().trim();
        prefs.put(KEY_CA_BUNDLE_PATH, path);
    }
}
