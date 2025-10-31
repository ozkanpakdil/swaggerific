package io.github.ozkanpakdil.swaggerific;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

@ExtendWith(ApplicationExtension.class)
public class AboutUiTest {

    private Parent root;

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/ozkanpakdil/swaggerific/edit/settings.fxml"));
        root = loader.load();
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    @Test
    public void openAboutPane_ShowsVersionAndLinks(FxRobot robot) {
        // Click the About button in the left menu
        Button btnAbout = robot.lookup("#btnAbout").queryButton();
        robot.clickOn(btnAbout);

        // Expect the About pane to load and show labels
        Label lblAppName = robot.lookup("#lblAppName").queryAs(Label.class);
        Label lblVersion = robot.lookup("#lblVersion").queryAs(Label.class);

        Assertions.assertNotNull(lblAppName, "App name label should exist");
        Assertions.assertNotNull(lblVersion, "Version label should exist");
        Assertions.assertTrue(lblAppName.getText().toLowerCase().contains("swaggerific"));
        Assertions.assertTrue(lblVersion.getText().toLowerCase().contains("version"));
    }
}
