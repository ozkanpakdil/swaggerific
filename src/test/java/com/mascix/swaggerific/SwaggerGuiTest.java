package com.mascix.swaggerific;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TextInputControlMatchers;
import org.testfx.service.support.CaptureSupport;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;

import static org.hamcrest.Matchers.containsString;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.testfx.api.FxService.serviceContext;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;

@ExtendWith(ApplicationExtension.class)
@Slf4j
@EnabledOnOs({ OS.WINDOWS })
class SwaggerGuiTest {

    private static MockServerClient mockServer;
    private static final CaptureSupport CAPTURE_SUPPORT = serviceContext().getCaptureSupport();

    /**
     * Will be called with {@code @Before} semantics, i. e. before each test method.
     *
     * @param stage - Will be injected by the test runner.
     */
    @Start
    private void start(Stage stage) throws IOException {
        String jsonBody = new String(Objects.requireNonNull(getClass().getResourceAsStream("/petstore-swagger.json")).readAllBytes());
        String findByStatusResponse = new String(Objects.requireNonNull(getClass().getResourceAsStream("/findbystatus-response.json")).readAllBytes());
        mockServer = startClientAndServer(1080);
        mockServer.hasStarted();
        mockServer.when(
                        request()
                                .withMethod("GET")
                                .withPath("/petstore-swagger.json")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(
                                        new Header("Content-Type", "application/json; charset=utf-8"),
                                        new Header("Cache-Control", "public, max-age=86400"))
                                .withBody(jsonBody)
                );

        mockServer.when(request("/petstore-pet/findByStatus")).respond(response(findByStatusResponse));

        Platform.isNestedLoopRunning();
        SwaggerApplication swaggerApplication = new SwaggerApplication();
        swaggerApplication.start(stage);
        stage.show();
    }

    @AfterAll
    public static void stopServer() {
        mockServer.stop();
    }

    @Test
    void click_treeview_call_get(FxRobot robot) {
        robot.push(KeyCode.CONTROL, KeyCode.O);
        robot.write("http://127.0.0.1:"+mockServer.getPort()+"/petstore-swagger.json");
        robot.push(KeyCode.ENTER);
        robot.sleep(500);
        robot.clickOn("#treePaths");
        robot.push(KeyCode.HOME);
        robot.push(KeyCode.RIGHT);
        robot.push(KeyCode.DOWN);
        robot.push(KeyCode.DOWN);
        robot.push(KeyCode.DOWN);
        robot.push(KeyCode.RIGHT);
        robot.push(KeyCode.DOWN);
//        getScreenShotOfTheTest(robot); // maybe used in the future
        robot.clickOn("#status").write("sold");
        robot.clickOn(".btnSend");
        FxAssert.verifyThat("#codeJsonResponse", isEnabled());
        robot.clickOn("#tabRaw");
        FxAssert.verifyThat("#codeRawJsonResponse", TextInputControlMatchers.hasText(containsString("id")));
        robot.clickOn("#statusBar");
        robot.clickOn("#statusBar");
    }

    private static void getScreenShotOfTheTest(FxRobot robot) {
        Image image = robot.capture(Screen.getPrimary().getBounds()).getImage();
        Path captureFile = Paths.get("screenshot" + new Date().getTime() + ".png");
        CAPTURE_SUPPORT.saveImage(image,captureFile);
    }
}