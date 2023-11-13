package com.mascix.swaggerific;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TextInputControlMatchers;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;

@ExtendWith(ApplicationExtension.class)
class SwaggerGuiTest {

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
    void click_treeview_call_get(FxRobot robot) {
        robot.push(KeyCode.CONTROL, KeyCode.O);
        robot.push(KeyCode.ENTER);
        robot.sleep(100);
        System.out.println(robot.lookup("#treePaths").query());
        robot.clickOn("#treePaths");
        robot.push(KeyCode.DOWN);
        robot.push(KeyCode.RIGHT);
        robot.push(KeyCode.RIGHT);
        robot.push(KeyCode.DOWN);
        robot.push(KeyCode.DOWN);
        robot.push(KeyCode.RIGHT);
        robot.push(KeyCode.DOWN);
        robot.clickOn("#status").write("sold");
//        robot.push(KeyCode.S,KeyCode.O,KeyCode.L,KeyCode.D);
        robot.clickOn(".btnSend");

//        FxAssert.verifyThat((NodeQuery) swaggerApplication, LabeledMatchers.hasText("click me!"));
        // or (lookup by css id):
        FxAssert.verifyThat("#codeJsonResponse", isEnabled());

        robot.clickOn(".tabRaw");
        // or (lookup by css class):
        FxAssert.verifyThat("#codeRawJsonResponse", TextInputControlMatchers.hasText(containsString("id")));
    }
}