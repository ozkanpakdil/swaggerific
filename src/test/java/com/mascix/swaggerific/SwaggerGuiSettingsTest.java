package com.mascix.swaggerific;

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
class SwaggerGuiSettingsTest {

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
    }

    @Test
    void openSettings(FxRobot robot) {
        robot.push(KeyCode.CONTROL, KeyCode.ALT, KeyCode.SHIFT, KeyCode.S);
        robot.clickOn("#btnGeneral");
        robot.clickOn("#btnThemes");
        robot.clickOn("#btnShortcuts");
        robot.clickOn("#btnData");
        robot.clickOn("#btnAddons");
        robot.clickOn("#btnCertificates");
        robot.clickOn("#btnProxy");
        robot.clickOn("#btnUpdate");
        robot.clickOn("#btnAbout");
        robot.push(KeyCode.ESCAPE);

        /*robot.push(KeyCode.ALT,KeyCode.E);
        robot.push(KeyCode.DOWN);
        robot.push(KeyCode.DOWN);
        robot.push(KeyCode.DOWN);
        robot.push(KeyCode.ENTER);
        robot.push(KeyCode.ESCAPE);*/
    }

}