package io.github.ozkanpakdil.swaggerific;

import io.github.ozkanpakdil.swaggerific.ui.textfx.CustomCodeArea;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.prefs.Preferences;

/**
 * UI test that verifies user-configured JSON folding shortcut works via scene accelerators.
 */
@ExtendWith(ApplicationExtension.class)
public class ShortcutsFoldingUiTest {

    private Parent settingsRoot;
    private final Preferences prefs = Preferences.userNodeForPackage(SwaggerApplication.class);

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/ozkanpakdil/swaggerific/edit/settings.fxml"));
        settingsRoot = loader.load();
        stage.setScene(new Scene(settingsRoot, 800, 600));
        stage.show();
    }

    @BeforeEach
    public void clearPrefs() {
        prefs.remove("shortcut.json.toggleFoldAtCaret");
        prefs.remove("shortcut.json.foldTop");
        prefs.remove("shortcut.json.unfoldAll");
    }

    @Test
    public void setToggleFoldShortcut_ThenFoldsInEditor(FxRobot robot) throws Exception {
        // Open Shortcuts pane
        Button btnShortcuts = robot.lookup("#btnShortcuts").queryButton();
        robot.clickOn(btnShortcuts);

        // Set JSON fold top-level to Ctrl+9
        robot.clickOn("#txtJsonFoldTop");
        robot.push(javafx.scene.input.KeyCode.CONTROL, javafx.scene.input.KeyCode.DIGIT9);
        robot.clickOn("Save");

        // Now open a simple editor scene and apply accelerators on FX thread
        final CustomCodeArea[] areaRef = new CustomCodeArea[1];
        final Scene[] sceneRef = new Scene[1];
        robot.interact(() -> {
            Stage stage = new Stage();
            CustomCodeArea area = new CustomCodeArea();
            area.replaceText("{\n  \"a\": 1,\n  \"b\": {\n    \"c\": 2\n  }\n}");
            Scene scene = new Scene(area, 400, 300);
            stage.setScene(scene);
            stage.show();
            areaRef[0] = area;
            sceneRef[0] = scene;
        });

        // Apply shortcuts to this new scene
        SwaggerApplication.applyCustomShortcutsToScene(sceneRef[0]);

        // Place caret on line 1 (after opening brace)
        robot.interact(() -> {
            areaRef[0].requestFocus();
            areaRef[0].moveTo(1);
        });

        // Instead of relying on key synthesis, invoke the installed accelerator directly
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userNodeForPackage(SwaggerApplication.class);
        String comboStr = p.get("shortcut.json.foldTop", "Ctrl+9");
        javafx.scene.input.KeyCodeCombination kc = io.github.ozkanpakdil.swaggerific.model.ShortcutModel.parseShortcut(comboStr);
        Runnable action = sceneRef[0].getAccelerators().get(kc);
        Assertions.assertNotNull(action, "Accelerator should be installed for fold top-level");
        robot.interact(action::run);

        // Verify folding placeholder appears
        String text = areaRef[0].getText();
        Assertions.assertTrue(text.contains(" … "), "Expected placeholder after folding via accelerator");
    }
}
