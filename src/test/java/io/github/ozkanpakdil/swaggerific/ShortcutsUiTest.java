package io.github.ozkanpakdil.swaggerific;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
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
public class ShortcutsUiTest {

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
    public void clearPrefs() {
        prefs.remove("shortcut.btnSendRequest");
    }

    @Test
    public void setSendRequestShortcut_PersistsToPreferences(FxRobot robot) {
        // Open Shortcuts pane
        Button btnShortcuts = robot.lookup("#btnShortcuts").queryButton();
        robot.clickOn(btnShortcuts);

        // Click the send request capture field
        robot.clickOn("#txtSendRequest");
        // Simulate typing a new shortcut like CTRL+ENTER
        robot.push(javafx.scene.input.KeyCode.CONTROL, javafx.scene.input.KeyCode.ENTER);

        // Click Save button by text
        robot.clickOn("Save");

        // Verify preference is set
        String pref = prefs.get("shortcut.btnSendRequest", null);
        Assertions.assertNotNull(pref, "Shortcut preference should be saved");
        // Accept either Ctrl+Enter or localized form
        Assertions.assertTrue(pref.toUpperCase().contains("CTRL") && pref.toUpperCase().contains("ENTER"),
                "Shortcut should contain CTRL and ENTER but was: " + pref);
    }
}
