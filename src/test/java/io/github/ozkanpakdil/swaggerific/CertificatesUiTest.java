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
public class CertificatesUiTest {

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
        prefs.remove("certs.caBundleEnabled");
        prefs.remove("certs.caBundlePath");
    }

    @Test
    public void setCertificatesPrefs_Persisted(FxRobot robot) {
        // Open Certificates pane
        Button btn = robot.lookup("#btnCertificates").queryButton();
        robot.clickOn(btn);

        // Toggle enable
        CheckBox chk = robot.lookup("#chkEnableCaBundle").queryAs(CheckBox.class);
        // Toggle once to enable and allow listeners to persist
        robot.clickOn(chk);
        robot.sleep(200);

        // Set path and press Enter
        TextField txt = robot.lookup("#txtCaBundlePath").queryAs(TextField.class);
        robot.clickOn(txt);
        robot.interact(() -> txt.setText("/tmp/ca.pem"));
        robot.push(javafx.scene.input.KeyCode.ENTER);
        robot.sleep(200);

        // Assert
        boolean enabled = prefs.getBoolean("certs.caBundleEnabled", false);
        String path = prefs.get("certs.caBundlePath", "");
        Assertions.assertTrue(enabled, "CA bundle should be enabled");
        Assertions.assertEquals("/tmp/ca.pem", path, "Path should persist");
    }
}
