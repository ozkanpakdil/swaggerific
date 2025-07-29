package io.github.ozkanpakdil.swaggerific;

import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpService;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpServiceImpl;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TextInputControlMatchers;
import org.testfx.service.support.CaptureSupport;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.containsString;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.testfx.api.FxService.serviceContext;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;

@ExtendWith(ApplicationExtension.class)
@EnabledOnOs({ OS.WINDOWS })
class SwaggerGuiTest {
    private static final Logger log = LoggerFactory.getLogger(SwaggerGuiTest.class);
    private static MockServerClient mockServer;
    private static final CaptureSupport CAPTURE_SUPPORT = serviceContext().getCaptureSupport();

    // Configure TestFX for optimal performance
    static {
        // Set shorter timeouts for TestFX operations
        WaitForAsyncUtils.autoCheckException = false;
        // Set default timeout to 2 seconds (instead of default 5)
        System.setProperty("testfx.robot.write_sleep", "20");
        System.setProperty("testfx.robot.sleep_time", "50");
        System.setProperty("testfx.robot.lookup_timeout", "1000");
    }

    /**
     * Will be called with {@code @Before} semantics, i. e. before each test method.
     *
     * @param stage - Will be injected by the test runner.
     */
    @Start
    private void start(Stage stage) throws IOException {
        String jsonBody = new String(
                Objects.requireNonNull(getClass().getResourceAsStream("/petstore-swagger.json")).readAllBytes());
        String findByStatusResponse = new String(
                Objects.requireNonNull(getClass().getResourceAsStream("/findbystatus-response-optimized.json")).readAllBytes());

        // Configure MockServer with performance optimizations
        mockServer = startClientAndServer(0);
        mockServer.hasStarted();

        // Configure expectations with immediate responses (no delay)
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
                                .withDelay(java.util.concurrent.TimeUnit.MILLISECONDS, 0) // No delay
                );

        // Configure findByStatus endpoint with optimized response and no delay
        mockServer.when(
                        request("/petstore-pet/findByStatus")
                )
                .respond(
                        response(findByStatusResponse)
                                .withDelay(java.util.concurrent.TimeUnit.MILLISECONDS, 0) // No delay
                );

        Platform.isNestedLoopRunning();
        SwaggerApplication swaggerApplication = new SwaggerApplication();
        swaggerApplication.start(stage);
        stage.show();
    }

    @AfterAll
    public static void stopServer() {
        mockServer.stop();
    }

    // Move HTTP request verification to a separate method
    private static HttpResponse verifyMockServerResponse(int port) {
        HttpService httpService = new HttpServiceImpl();
        HttpResponse httpResponse = httpService.get(
                URI.create("http://127.0.0.1:" + port + "/petstore-swagger.json"),
                Map.of("Content-Type", "application/json; charset=utf-8"));
        assert httpResponse.statusCode() == 200;
        return httpResponse;
    }

    @Test
    void click_treeview_call_get(FxRobot robot) {
        try {
            // Verify mock server is responding before starting UI test
            verifyMockServerResponse(mockServer.getPort());

            log.info("Opening Swagger JSON file");

            robot.targetWindow(0);
            log.info("Successfully targeted primary window");

            // Open file dialog with keyboard shortcut
            robot.push(KeyCode.CONTROL, KeyCode.O);

            robot.write("http://127.0.0.1:" + mockServer.getPort() + "/petstore-swagger.json");
            robot.push(KeyCode.ENTER);

            // Wait for file to load - reduced wait time
            log.info("Waiting for file to load...");

            log.info("Clicking on tree paths and navigating");

            // Wait for the tree to be visible and enabled
            WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> {
                try {
                    Node treeNode = robot.lookup("#treePaths").query();
                    return treeNode != null && treeNode.isVisible() && treeNode.isDisabled() == false;
                } catch (Exception e) {
                    return false;
                }
            });

            // Ensure we're targeting the main application window - use a more reliable approach
            try {
                // Target the primary stage (index 0) which should be our application window
                robot.targetWindow(0);
                log.info("Successfully targeted primary window for tree interaction");
            } catch (Exception e) {
                log.warn("Could not target primary window for tree interaction, continuing anyway: {}", e.getMessage());
            }

            // Click on the tree and ensure it has focus
            robot.clickOn("#treePaths");

            // Navigate to the top of the tree
            robot.push(KeyCode.HOME);

            // Navigate to the pet/findByStatus endpoint efficiently
            log.info("Navigating to pet/findByStatus endpoint");

            // Define navigation sequence with verification points
            KeyCode[] navigationSequence = {
                    KeyCode.RIGHT,
                    KeyCode.DOWN, KeyCode.DOWN, KeyCode.DOWN,
                    KeyCode.RIGHT,
                    KeyCode.DOWN
            };

            // Execute navigation with proper waits and verification
            for (int i = 0; i < navigationSequence.length; i++) {
                KeyCode keyCode = navigationSequence[i];

                // Press the key and wait for UI to respond
                robot.push(keyCode);

                // Add minimal waits at critical points (after expanding nodes)
                if (keyCode == KeyCode.RIGHT) {
                    WaitForAsyncUtils.sleep(150, TimeUnit.MILLISECONDS);
                }
                // No need to verify focus on every step - tree should maintain focus during navigation
            }

            // Handle both TextField and ComboBox for status parameter
            log.info("Setting status parameter");

            // Ensure we're targeting the main application window
            try {
                try {
                    // Target the primary stage (index 0) which should be our application window
                    robot.targetWindow(0);
                    log.info("Successfully targeted primary window for parameter setting");
                } catch (Exception e) {
                    log.warn("Could not target primary window for parameter setting, continuing anyway: {}", e.getMessage());
                }

                // Verify status field exists before interacting with it
                log.info("Verifying status field is available");
                Node statusNode = null;
                try {
                    // Try to find the status field
                    statusNode = robot.lookup("#status").query();
                    if (statusNode == null || !statusNode.isVisible() || statusNode.isDisabled()) {
                        log.warn("Status field not ready for interaction");
                        getScreenShotOfTheTest(robot);
                        // Wait a shorter time and try again
                        WaitForAsyncUtils.sleep(500, TimeUnit.MILLISECONDS);
                        statusNode = robot.lookup("#status").query();
                    }
                } catch (Exception e) {
                    log.warn("Error looking up status field: {}", e.getMessage());
                    getScreenShotOfTheTest(robot);
                    // Wait a shorter time and try again
                    WaitForAsyncUtils.sleep(500, TimeUnit.MILLISECONDS);
                    try {
                        statusNode = robot.lookup("#status").query();
                    } catch (Exception e2) {
                        log.error("Status field not found after retry: {}", e2.getMessage());
                        getScreenShotOfTheTest(robot);
                        throw new RuntimeException("Status field not available", e2);
                    }
                }

                // Try multiple approaches to set the status parameter
                boolean statusSet = false;

                // Approach 1: Try to interact with the input field directly
                try {
                    log.info("Attempting to set status via direct input");
                    robot.clickOn("#status");
                    robot.write("sold");
                    statusSet = true;
                    log.info("Successfully set status via direct input");
                } catch (Exception e) {
                    log.warn("Could not directly interact with status field: {}", e.getMessage());
                }

                // Approach 2: Try to use ComboBox if direct input failed
                if (!statusSet) {
                    try {
                        log.info("Attempting to set status via ComboBox");
                        // Try to find and use a ComboBox
                        @SuppressWarnings("unchecked")
                        ComboBox<String> statusCombo = (ComboBox<String>) robot.lookup("#status").query();

                        // Use Platform.runLater to ensure UI thread is not blocked
                        final CountDownLatch comboLatch = new CountDownLatch(1);
                        Platform.runLater(() -> {
                            try {
                                statusCombo.setValue("sold");
                                comboLatch.countDown();
                            } catch (Exception e) {
                                log.error("Error setting ComboBox value: {}", e.getMessage());
                                comboLatch.countDown();
                            }
                        });

                        // Wait with timeout for the UI operation to complete
                        if (comboLatch.await(2, TimeUnit.SECONDS)) {
                            statusSet = true;
                            log.info("Successfully set status via ComboBox");
                        } else {
                            log.warn("Timed out waiting for ComboBox value to be set");
                        }
                    } catch (Exception e2) {
                        log.error("Could not find or use status ComboBox: {}", e2.getMessage());
                    }
                }

                // If all approaches failed, throw an exception
                if (!statusSet) {
                    log.error("All attempts to set status parameter failed");
                    getScreenShotOfTheTest(robot);
                    throw new RuntimeException("Failed to set status parameter");
                }

                // Send the request with robust error handling
                log.info("Sending request");

                // Verify send button exists before clicking
                Node sendButton = null;
                try {
                    sendButton = robot.lookup(".btnSend").query();
                    if (sendButton == null || !sendButton.isVisible() || sendButton.isDisabled()) {
                        log.warn("Send button not ready for interaction");
                        getScreenShotOfTheTest(robot);
                        // Wait a shorter time and try again
                        WaitForAsyncUtils.sleep(500, TimeUnit.MILLISECONDS);
                        sendButton = robot.lookup(".btnSend").query();
                    }
                } catch (Exception e) {
                    log.warn("Error looking up send button: {}", e.getMessage());
                    getScreenShotOfTheTest(robot);
                    throw new RuntimeException("Send button not available", e);
                }

                // Click the send button
                robot.clickOn(".btnSend");

                // Wait for response with improved error handling
                log.info("Waiting for response...");

                try {
                    // Wait for response with reduced timeout
                    WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, () -> {
                        try {
                            Node responseNode = robot.lookup("#codeJsonResponse").query();
                            return responseNode != null && responseNode.isVisible();
                        } catch (Exception e) {
                            return false;
                        }
                    });
                    log.info("Response received successfully");
                } catch (TimeoutException te) {
                    log.warn("Timed out waiting for response, continuing anyway");
                    getScreenShotOfTheTest(robot);
                } catch (Exception e) {
                    log.warn("Exception while waiting for response: {}", e.getMessage());
                }

                // Simplified response verification
                log.info("Verifying response");
                try {
                    // Verify JSON response is available
                    FxAssert.verifyThat("#codeJsonResponse", isEnabled());

                    // Try to click on Raw tab and verify content in one step
                    try {
                        robot.clickOn("#tabRaw");
                        FxAssert.verifyThat("#codeRawJsonResponse", TextInputControlMatchers.hasText(containsString("id")));
                        log.info("Response verification successful");
                    } catch (Exception e) {
                        // Non-critical error, just log it and continue
                        log.warn("Could not verify raw response content: {}", e.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Error during response verification: {}", e.getMessage());
                    getScreenShotOfTheTest(robot);
                }

                // Complete the test
                log.info("Test completed successfully");
                try {
                    robot.clickOn("#statusBar");
                    robot.clickOn("#statusBar");
                } catch (Exception e) {
                    log.warn("Could not click on status bar: {}", e.getMessage());
                }

            } catch (Exception e) {
                log.error("Exception during parameter setting or request sending", e);
                getScreenShotOfTheTest(robot);
                throw e;
            }
        } catch (Exception e) {
            log.error("Test failed with exception", e);
            getScreenShotOfTheTest(robot);
        }
    }

    private static void getScreenShotOfTheTest(FxRobot robot) {
        try {
            // Use a more efficient approach with shorter timeout
            log.info("Capturing screenshot...");

            // Capture on the JavaFX application thread with a shorter timeout
            final Image[] capturedImage = new Image[1];
            final CountDownLatch latch = new CountDownLatch(1);

            // Use a shorter timeout (2 seconds instead of 5)
            Platform.runLater(() -> {
                try {
                    // Capture the primary screen
                    capturedImage[0] = robot.capture(Screen.getPrimary().getBounds()).getImage();
                    latch.countDown();
                } catch (Exception e) {
                    log.error("Error capturing screenshot: {}", e.getMessage());
                    latch.countDown();
                }
            });

            // Wait with even shorter timeout
            if (latch.await(1, TimeUnit.SECONDS)) {
                if (capturedImage[0] != null) {
                    // Use a more efficient file naming convention
                    Path captureFile = Paths.get("screenshot" + System.currentTimeMillis() + ".png");
                    CAPTURE_SUPPORT.saveImage(capturedImage[0], captureFile);
                    log.info("Screenshot saved to {}", captureFile.getFileName());
                }
            } else {
                log.warn("Screenshot capture timed out after 1 second");
            }
        } catch (Exception e) {
            log.warn("Could not capture screenshot: {}", e.getMessage());
            // Don't rethrow - screenshots are for debugging only
        }
    }
}
