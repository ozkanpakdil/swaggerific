package io.github.ozkanpakdil.swaggerific.ui.edit;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class Themes implements Initializable {

    public static final String KEY_UI_THEME = "ui.theme"; // values: light, dark
    public static final String KEY_UI_THEME_APPLY_IMMEDIATELY = "ui.theme.applyImmediately";

    @FXML
    private ComboBox<String> cmbTheme;
    @FXML
    private CheckBox chkApplyImmediately;

    private final Preferences prefs = Preferences.userNodeForPackage(io.github.ozkanpakdil.swaggerific.SwaggerApplication.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Populate theme choices
        cmbTheme.getItems().setAll("Light", "Dark");
        String theme = prefs.get(KEY_UI_THEME, "light");
        cmbTheme.getSelectionModel().select("dark".equalsIgnoreCase(theme) ? "Dark" : "Light");

        boolean applyNow = prefs.getBoolean(KEY_UI_THEME_APPLY_IMMEDIATELY, true);
        chkApplyImmediately.setSelected(applyNow);
    }

    @FXML
    public void onThemeChanged() {
        String sel = cmbTheme.getSelectionModel().getSelectedItem();
        String value = (sel != null && sel.equalsIgnoreCase("Dark")) ? "dark" : "light";
        prefs.put(KEY_UI_THEME, value);
        if (chkApplyImmediately.isSelected()) {
            applyTheme(value);
        }
    }

    @FXML
    public void onApplyImmediatelyChanged() {
        boolean applyNow = chkApplyImmediately.isSelected();
        prefs.putBoolean(KEY_UI_THEME_APPLY_IMMEDIATELY, applyNow);
        if (applyNow) {
            // Apply current selection immediately
            String sel = cmbTheme.getSelectionModel().getSelectedItem();
            String value = (sel != null && sel.equalsIgnoreCase("Dark")) ? "dark" : "light";
            applyTheme(value);
        }
    }

    private void applyTheme(String value) {
        boolean dark = "dark".equalsIgnoreCase(value);
        if (dark) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }
        // Toggle dark accessibility fixes (e.g., TreeView contrast)
        try {
            SwaggerApplication app = SwaggerApplication.getInstance();
            if (app != null) {
                app.applyDarkAccessibilityFixes(dark);
            }
        } catch (Exception ignored) { }
    }
}
