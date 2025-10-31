package io.github.ozkanpakdil.swaggerific;

import io.github.ozkanpakdil.swaggerific.ui.edit.SettingsController;
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

import static io.github.ozkanpakdil.swaggerific.ui.edit.Themes.KEY_UI_THEME;
import static io.github.ozkanpakdil.swaggerific.ui.edit.Themes.KEY_UI_THEME_APPLY_IMMEDIATELY;

@ExtendWith(ApplicationExtension.class)
public class SettingsUiTest {

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
    public void clearThemePrefs() {
        prefs.remove(KEY_UI_THEME);
        prefs.remove(KEY_UI_THEME_APPLY_IMMEDIATELY);
    }

    @Test
    public void changeThemeToDark_ApplyImmediately_SavesPrefs(FxRobot robot) {
        // Click Themes button on the left
        Button btnThemes = robot.lookup("#btnThemes").queryButton();
        robot.clickOn(btnThemes);

        // Change to Dark
        @SuppressWarnings("unchecked")
        ComboBox<String> cmbTheme = robot.lookup("#cmbTheme").queryAs(ComboBox.class);
        robot.interact(() -> {
            cmbTheme.getSelectionModel().select("Dark");
        });

        // Ensure Apply Immediately is persisted: toggle it off and on to trigger handler
        CheckBox chkApply = robot.lookup("#chkApplyImmediately").queryAs(CheckBox.class);
        robot.clickOn(chkApply); // toggle
        robot.clickOn(chkApply); // toggle back to selected

        // Assert preferences updated
        String theme = prefs.get(KEY_UI_THEME, "light");
        boolean apply = prefs.getBoolean(KEY_UI_THEME_APPLY_IMMEDIATELY, false);
        Assertions.assertEquals("dark", theme);
        Assertions.assertTrue(apply);
    }
}
