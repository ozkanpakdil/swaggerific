package io.github.ozkanpakdil.swaggerific;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.IOException;

@ExtendWith(ApplicationExtension.class)
@EnabledOnOs({ OS.WINDOWS })
class SwaggerGuiMenuTest {

    /**
     * Will be called with {@code @Before} semantics, i. e. before each test method.
     *
     * @param stage - Will be injected by the test runner.
     */
    @Start
    private void start(Stage stage) throws IOException {
        Platform.isNestedLoopRunning();
        SwaggerApplication swaggerApplication = new SwaggerApplication();
        swaggerApplication.start(stage);
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    @Test
    void openViewAndHelpMenu(FxRobot robot) {
        robot.push(KeyCode.ALT, KeyCode.V, KeyCode.D);
        robot.push(KeyCode.ALT, KeyCode.V, KeyCode.T);
        robot.push(KeyCode.ALT, KeyCode.V, KeyCode.F);
        robot.push(KeyCode.ALT, KeyCode.V, KeyCode.S);
        robot.push(KeyCode.ALT, KeyCode.V, KeyCode.E);
        robot.push(KeyCode.ALT, KeyCode.V, KeyCode.C);
        robot.push(KeyCode.ALT, KeyCode.H, KeyCode.A);
        robot.push(KeyCode.ALT, KeyCode.H, KeyCode.R);
    }

    @Test
    void openJson(FxRobot robot) {
        robot.push(KeyCode.CONTROL, KeyCode.O);
        robot.push(KeyCode.ESCAPE);
        robot.push(KeyCode.CONTROL, KeyCode.O);
        robot.push(KeyCode.ENTER);
    }

}