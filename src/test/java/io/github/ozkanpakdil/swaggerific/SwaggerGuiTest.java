package io.github.ozkanpakdil.swaggerific;

import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpService;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpServiceImpl;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TextInputControlMatchers;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.Matchers.containsString;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;

@ExtendWith(ApplicationExtension.class)
@EnabledOnOs({ OS.WINDOWS })
class SwaggerGuiTest {
    private static final Logger log = LoggerFactory.getLogger(SwaggerGuiTest.class);
    private static SimpleHttpServer httpServer;
    //    private static final CaptureSupport CAPTURE_SUPPORT = serviceContext().getCaptureSupport();

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

    @BeforeAll
    public static void startServer() throws IOException {
        // Read JSON files from resources
        String jsonBody = new String(
                Objects.requireNonNull(SwaggerGuiTest.class.getResourceAsStream("/petstore-swagger.json")).readAllBytes());
        String findByStatusResponse = new String(
                Objects.requireNonNull(SwaggerGuiTest.class.getResourceAsStream("/findbystatus-response-optimized.json")).readAllBytes());
        
        // Create and configure the SimpleHttpServer
        httpServer = new SimpleHttpServer();
        
        // Configure responses
        httpServer.addResponse(
                "/petstore-swagger.json", 
                jsonBody, 
                "application/json; charset=utf-8", 
                200);
        
        httpServer.addResponse(
                "/petstore-pet/findByStatus", 
                findByStatusResponse, 
                "application/json", 
                200);
        
        // Start the server
        httpServer.start();
        
        log.info("SimpleHttpServer started on port: {}", httpServer.getPort());
    }

    @AfterAll
    public static void stopServer() {
        if (httpServer != null) {
            httpServer.stop();
            log.info("SimpleHttpServer stopped");
        }
    }

    @Test
    void click_treeview_call_get(FxRobot robot) {
        // set up http request to send to http server and receive response
        HttpService httpService = new HttpServiceImpl();
        HttpResponse httpResponse = httpService.get(
                URI.create("http://127.0.0.1:" + httpServer.getPort() + "/petstore-swagger.json"),
                Map.of("Content-Type", "application/json; charset=utf-8"));
        assert httpResponse.statusCode() == 200;

        log.info("Pressing CTRL+O to open the Swagger file in the GUI");
        robot.push(KeyCode.CONTROL, KeyCode.O);
        robot.write("http://127.0.0.1:" + httpServer.getPort() + "/petstore-swagger.json");
        robot.push(KeyCode.ENTER);
        robot.sleep(1000); // Give time for the file to load
        robot.clickOn("#treePaths");
        robot.push(KeyCode.HOME);

        // Navigate to the pet/findByStatus endpoint
        log.info("Navigating to pet/findByStatus endpoint");
        try {
            // Define navigation sequence
            KeyCode[] navigationSequence = {
                KeyCode.RIGHT, 
                KeyCode.DOWN, KeyCode.DOWN, KeyCode.DOWN, 
                KeyCode.RIGHT, 
                KeyCode.DOWN
            };
            
            // Execute navigation with proper waits
            for (KeyCode keyCode : navigationSequence) {
                robot.push(keyCode);
                robot.sleep(300); // Wait between key presses
            }
            
            log.info("Setting status parameter");
            try {
                // Try to interact with the input field directly
                robot.clickOn("#status");
                robot.sleep(200);
                robot.write("sold");
                log.info("Successfully set status via direct input");
            } catch (Exception e) {
                log.warn("Could not directly interact with status field: {}", e.getMessage());
                
                // Try alternative approaches
                try {
                    // Try to find and use a ComboBox
                    @SuppressWarnings("unchecked")
                    ComboBox<String> statusCombo = (ComboBox<String>) robot.lookup("#status").query();
                    
                    // Set the value directly
                    statusCombo.setValue("sold");
                    robot.sleep(500); // Give time for the value to be set
                    log.info("Successfully set status via ComboBox");
                } catch (Exception e2) {
                    log.error("Could not find or use status input field: {}", e2.getMessage());
                    throw new RuntimeException("Failed to set status parameter", e2);
                }
            }
            
            log.info("Sending request");
            robot.clickOn(".btnSend");
            robot.sleep(2000); // Give time for the request to complete
            
            log.info("Verifying response");
            FxAssert.verifyThat("#codeJsonResponse", isEnabled());
            robot.clickOn("#tabRaw");
            robot.sleep(500);
            FxAssert.verifyThat("#codeRawJsonResponse", TextInputControlMatchers.hasText(containsString("id")));
            
            log.info("Test completed successfully");
            robot.clickOn("#statusBar");
            robot.sleep(300);
            robot.clickOn("#statusBar");
        } catch (Exception e) {
            log.error("Failed during UI interaction", e);
            throw e;
        }
    }

    //    private static void getScreenShotOfTheTest(FxRobot robot) {
    //        Image image = robot.capture(Screen.getPrimary().getBounds()).getImage();
    //        Path captureFile = Paths.get("screenshot" + new Date().getTime() + ".png");
    //        CAPTURE_SUPPORT.saveImage(image, captureFile);
    //    }
}
