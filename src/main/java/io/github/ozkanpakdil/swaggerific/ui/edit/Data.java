package io.github.ozkanpakdil.swaggerific.ui.edit;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class Data implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(Data.class);

    public static final String KEY_SAVE_HISTORY = "ui.data.saveHistory";
    public static final String KEY_HISTORY_RETENTION_DAYS = "ui.data.historyRetentionDays"; // int

    @FXML private CheckBox chkSaveHistory;
    @FXML private TextField txtRetentionDays;

    private final Preferences prefs = Preferences.userNodeForPackage(io.github.ozkanpakdil.swaggerific.SwaggerApplication.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chkSaveHistory.setSelected(prefs.getBoolean(KEY_SAVE_HISTORY, true));
        txtRetentionDays.setText(String.valueOf(prefs.getInt(KEY_HISTORY_RETENTION_DAYS, 30)));
    }

    @FXML
    public void onSaveHistoryChanged() {
        boolean val = chkSaveHistory.isSelected();
        prefs.putBoolean(KEY_SAVE_HISTORY, val);
        log.info("Save history set to {}", val);
    }

    @FXML
    public void onRetentionDaysChanged() {
        try {
            int days = Integer.parseInt(txtRetentionDays.getText().trim());
            if (days < 0) throw new NumberFormatException();
            prefs.putInt(KEY_HISTORY_RETENTION_DAYS, days);
        } catch (NumberFormatException e) {
            log.warn("Invalid retention days: {}", txtRetentionDays.getText());
        }
    }
}
