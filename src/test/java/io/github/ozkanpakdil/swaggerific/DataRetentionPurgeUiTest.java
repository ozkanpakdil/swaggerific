package io.github.ozkanpakdil.swaggerific;

import io.github.ozkanpakdil.swaggerific.tools.history.HistoryService;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.prefs.Preferences;

@ExtendWith(ApplicationExtension.class)
public class DataRetentionPurgeUiTest {

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
    public void clearPrefsAndDir() throws Exception {
        prefs.putBoolean("ui.data.saveHistory", true);
        prefs.putInt("ui.data.historyRetentionDays", 1); // default 1 day for tests
        // Ensure history dir exists and is empty
        HistoryService.ensureDir();
        Path dir = HistoryService.getBaseDir();
        if (Files.exists(dir)) {
            Files.list(dir).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            });
        }
    }

    @Test
    public void purgeRemovesOldFiles_KeepsRecent(FxRobot robot) throws Exception {
        // Open Data pane and set retention to 1 day via UI (and ensure Save History enabled)
        Button btnData = robot.lookup("#btnData").queryButton();
        robot.clickOn(btnData);
        CheckBox chk = robot.lookup("#chkSaveHistory").queryAs(CheckBox.class);
        if (!chk.isSelected()) robot.clickOn(chk);
        TextField txtDays = robot.lookup("#txtRetentionDays").queryAs(TextField.class);
        robot.interact(() -> txtDays.setText("1"));
        robot.push(javafx.scene.input.KeyCode.ENTER);

        Path dir = HistoryService.getBaseDir();
        // Create two files: one old (3 days ago), one recent (now)
        Path oldFile = dir.resolve("20200101-000000-GET-localhost.json");
        Files.writeString(oldFile, "{}\n");
        // set last modified time to 3 days ago
        File oldF = oldFile.toFile();
        oldF.setLastModified(System.currentTimeMillis() - 3L * 24L * 60L * 60L * 1000L);

        Path recentFile = dir.resolve("20200102-000000-GET-localhost.json");
        Files.writeString(recentFile, "{}\n");
        // make sure it's current time
        recentFile.toFile().setLastModified(Instant.now().toEpochMilli());

        // Run purge
        int deleted = HistoryService.purgeOld();

        // Assert old was deleted, recent remains
        Assertions.assertTrue(deleted >= 1, "Expected at least one file purged");
        Assertions.assertFalse(Files.exists(oldFile), "Old file should be deleted");
        Assertions.assertTrue(Files.exists(recentFile), "Recent file should remain");
    }
}
