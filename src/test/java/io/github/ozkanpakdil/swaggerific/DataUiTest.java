package io.github.ozkanpakdil.swaggerific;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.prefs.Preferences;

@ExtendWith(ApplicationExtension.class)
public class DataUiTest {

    private Parent root;
    private final Preferences prefs = Preferences.userNodeForPackage(SwaggerApplication.class);

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/ozkanpakdil/swaggerific/edit/settings.fxml"));
        root = loader.load();
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    @BeforeEach
    public void clearPrefs() {
        prefs.remove("ui.data.saveHistory");
        prefs.remove("ui.data.historyRetentionDays");
    }

    @Test
    public void changeDataPrefs_Persisted(FxRobot robot) {
        // Open Data pane
        Button btnData = robot.lookup("#btnData").queryButton();
        robot.clickOn(btnData);
        robot.sleep(250);

        // Ensure save history is ON and persist via handler
        CheckBox chk = robot.lookup("#chkSaveHistory").queryAs(CheckBox.class);
        robot.interact(() -> chk.setSelected(true));
        robot.clickOn(chk); // fire onAction to persist current state

        // Set retention to 15 days and press Enter (onAction)
        TextField txt = robot.lookup("#txtRetentionDays").queryAs(TextField.class);
        robot.clickOn(txt);
        robot.interact(() -> txt.setText("15"));
        robot.push(javafx.scene.input.KeyCode.ENTER);

        // Verify
        boolean saveHistory = prefs.getBoolean("ui.data.saveHistory", true);
        int days = prefs.getInt("ui.data.historyRetentionDays", -1);
        Assertions.assertFalse(saveHistory, "Save history should be false after one toggle click");
        Assertions.assertEquals(15, days, "Retention days should be 15");
    }
}
