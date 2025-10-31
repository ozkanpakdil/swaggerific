package io.github.ozkanpakdil.swaggerific.ui.edit;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Settings controller for Update preferences.
 */
public class Update implements Initializable {

    public static final String KEY_CHECK_ON_STARTUP = "ui.update.checkOnStartup";
    public static final String KEY_UPDATE_CHANNEL = "ui.update.channel"; // stable|beta

    @FXML
    private CheckBox chkCheckOnStartup;
    @FXML
    private ComboBox<String> cmbChannel;

    private final Preferences prefs = Preferences.userNodeForPackage(io.github.ozkanpakdil.swaggerific.SwaggerApplication.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Populate channels
        cmbChannel.getItems().setAll("Stable", "Beta");

        // Load values
        boolean check = prefs.getBoolean(KEY_CHECK_ON_STARTUP, false);
        String channel = prefs.get(KEY_UPDATE_CHANNEL, "stable");

        chkCheckOnStartup.setSelected(check);
        cmbChannel.getSelectionModel().select("beta".equalsIgnoreCase(channel) ? "Beta" : "Stable");
    }

    @FXML
    public void onCheckOnStartupChanged() {
        prefs.putBoolean(KEY_CHECK_ON_STARTUP, chkCheckOnStartup.isSelected());
    }

    @FXML
    public void onChannelChanged() {
        String sel = cmbChannel.getSelectionModel().getSelectedItem();
        String value = (sel != null && sel.equalsIgnoreCase("Beta")) ? "beta" : "stable";
        prefs.put(KEY_UPDATE_CHANNEL, value);
    }
}
