package io.github.ozkanpakdil.swaggerific;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
public class AddonsUiTest {

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
        prefs.remove("ui.addons.enabled");
    }

    @Test
    public void toggleAddons_Persisted(FxRobot robot) {
        Button btnAddons = robot.lookup("#btnAddons").queryButton();
        robot.clickOn(btnAddons);

        CheckBox chk = robot.lookup("#chkEnableAddons").queryAs(CheckBox.class);
        robot.clickOn(chk);

        boolean enabled = prefs.getBoolean("ui.addons.enabled", false);
        Assertions.assertTrue(enabled, "Addons enabled should be true after toggle");
    }
}
