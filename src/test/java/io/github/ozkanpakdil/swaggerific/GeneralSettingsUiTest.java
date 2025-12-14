package io.github.ozkanpakdil.swaggerific;

import io.github.ozkanpakdil.swaggerific.ui.edit.General;
import atlantafx.base.controls.ToggleSwitch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;
import org.testfx.framework.junit5.Start;

import java.util.prefs.Preferences;

@ExtendWith(ApplicationExtension.class)
public class GeneralSettingsUiTest {

    private Parent root;
    private final Preferences prefs = Preferences.userNodeForPackage(SwaggerApplication.class);

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/io/github/ozkanpakdil/swaggerific/edit/settings.fxml"));
        root = fxmlLoader.load();
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    @BeforeEach
    public void resetPref() {
        prefs.remove(General.KEY_ASK_WHEN_CLOSING_UNSAVED);
    }

    @Test
    public void toggleAskWhenClosingUnsaved_UpdatesPreference(FxRobot robot) {
        // Ensure we're on General tab
        Button btnGeneral = robot.lookup("#btnGeneral").queryButton();
        robot.clickOn(btnGeneral);

        // Default is true; programmatically change selection to OFF and let listener persist
        ToggleSwitch toggle = robot.lookup("#chkAlwaysAskWhenClosingUnsavedTabs").queryAs(ToggleSwitch.class);
        robot.interact(() -> toggle.setSelected(false));
        WaitForAsyncUtils.waitForFxEvents();

        boolean value = prefs.getBoolean(General.KEY_ASK_WHEN_CLOSING_UNSAVED, true);
        // After toggle once, it should be false
        Assertions.assertFalse(value, "Preference should be false after disabling toggle");

        // Programmatically toggle back to true and persist
        robot.interact(() -> toggle.setSelected(true));
        WaitForAsyncUtils.waitForFxEvents();
        boolean value2 = prefs.getBoolean(General.KEY_ASK_WHEN_CLOSING_UNSAVED, false);
        Assertions.assertTrue(value2, "Preference should be true after enabling toggle again");
    }
}
