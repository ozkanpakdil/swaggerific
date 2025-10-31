package io.github.ozkanpakdil.swaggerific;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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
public class UpdateUiTest {

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
        prefs.remove("ui.update.checkOnStartup");
        prefs.remove("ui.update.channel");
    }

    @Test
    public void setUpdatePrefs_Persisted(FxRobot robot) {
        // Open Update pane
        Button btnUpdate = robot.lookup("#btnUpdate").queryButton();
        robot.clickOn(btnUpdate);

        // Toggle check on startup
        CheckBox chk = robot.lookup("#chkCheckOnStartup").queryAs(CheckBox.class);
        robot.clickOn(chk); // should set to true

        // Select channel Beta
        @SuppressWarnings("unchecked")
        ComboBox<String> cmb = robot.lookup("#cmbChannel").queryAs(ComboBox.class);
        robot.interact(() -> cmb.getSelectionModel().select("Beta"));

        // Assert preferences
        boolean checkOnStartup = prefs.getBoolean("ui.update.checkOnStartup", false);
        String channel = prefs.get("ui.update.channel", "stable");
        Assertions.assertTrue(checkOnStartup, "Check on startup should be true");
        Assertions.assertEquals("beta", channel, "Channel should be saved as beta");
    }
}
