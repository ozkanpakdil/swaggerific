package com.mascix.swaggerific;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TextInputControlMatchers;

import java.io.IOException;
import java.util.Objects;

import static org.hamcrest.Matchers.containsString;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;

@ExtendWith(ApplicationExtension.class)
class SwaggerGuiTest {

    private static MockServerClient mockServer;

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
        robot.write("http://127.0.0.1:1080/petstore-swagger.json");
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