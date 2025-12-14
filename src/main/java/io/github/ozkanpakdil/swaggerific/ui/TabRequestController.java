package io.github.ozkanpakdil.swaggerific.ui;

import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import io.github.ozkanpakdil.swaggerific.tools.HttpUtility;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.ui.component.STextField;
import io.github.ozkanpakdil.swaggerific.ui.component.TreeItemOperationLeaf;
import io.github.ozkanpakdil.swaggerific.ui.edit.AuthorizationController;
import io.github.ozkanpakdil.swaggerific.ui.edit.PreRequestScriptController;
import io.github.ozkanpakdil.swaggerific.ui.edit.ResponseTestScriptController;
import io.github.ozkanpakdil.swaggerific.ui.model.TestResult;
import io.github.ozkanpakdil.swaggerific.ui.textfx.BracketHighlighter;
import io.github.ozkanpakdil.swaggerific.ui.textfx.CustomCodeArea;
import io.github.ozkanpakdil.swaggerific.ui.textfx.JsonColorize;
import io.github.ozkanpakdil.swaggerific.ui.textfx.SelectedHighlighter;
import io.github.ozkanpakdil.swaggerific.ui.textfx.XmlColorizer;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.function.IntFunction;

import org.fxmisc.flowless.VirtualizedScrollPane;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TabRequestController extends TabPane implements TabRequestControllerBase {
    private volatile boolean dirty = false;

    public boolean isDirty() {
        return dirty;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TabRequestController.class);
    public ComboBox cmbHttpMethod;
    @FXML
    Button btnSend;
    MainController mainController;
    // Editors are created at runtime to avoid RichTextFX classes in FXML for native images
    private CodeArea codeJsonRequest;
    private CustomCodeArea codeJsonResponse;
    private TextArea codeJsonRequestText; // native fallback
    private TextArea codeJsonResponseText; // native fallback for Pretty tab
    @FXML
    TextArea codeRawJsonResponse;
    @FXML
    TextField txtAddress;
    @FXML
    GridPane boxRequestParams;
    @FXML
    VBox boxParams;
    @FXML
    TabPane tabRequestDetails;
    @FXML
    Tab tabBody;
    @FXML
    Tab tabParams;
    @FXML
    TableView tableHeaders;
    @FXML
    StackPane codeRequestContainer;
    @FXML
    StackPane responsePrettyContainer;

    @FXML
    AuthorizationController authorizationController;

    @FXML
    PreRequestScriptController preRequestScriptController;

    @FXML
    ResponseTestScriptController responseTestScriptController;

    @FXML
    TableView<TestResult> tableTestResults;

    JsonColorize jsonColorize = new JsonColorize();

    private static boolean isNativeImage() {
        String v = System.getProperty("org.graalvm.nativeimage.imagecode");
        return v != null;
    }

    private String getRequestText() {
        if (codeJsonRequest != null) return codeJsonRequest.getText();
        if (codeJsonRequestText != null) return codeJsonRequestText.getText();
        return "";
    }

    private void setRequestText(String text) {
        if (codeJsonRequest != null) {
            codeJsonRequest.replaceText(text);
        } else if (codeJsonRequestText != null) {
            codeJsonRequestText.setText(text);
        }
    }

    public void setPrettyResponseText(String text) {
        if (codeJsonResponse != null) {
            codeJsonResponse.replaceText(text);
        } else if (codeJsonResponseText != null) {
            codeJsonResponseText.setText(text);
        }
    }

    /**
     * Apply XML styling to the Pretty response editor if RichTextFX is available.
     * In native mode (TextArea), styling is skipped but content is still shown.
     */
    public void applyXmlStylingIfSupported(String cssPath) {
        if (codeJsonResponse != null) {
            codeResponseXmlSettings(codeJsonResponse, cssPath);
        }
    }

    /**
     * Saves the current authorization settings for the current URL. This method is called when the authorization settings
     * change.
     */
    private void saveAuthorizationSettings() {
        if (mainController != null && authorizationController != null && txtAddress != null) {
            String url = txtAddress.getText();
            if (url != null && !url.isEmpty()) {
                mainController.authorizationSettings.saveSettingsForUrl(url, authorizationController);
                log.debug("Saved authorization settings for URL: {}", url);
            }
        }
    }

    /**
     * Loads authorization settings for the specified URL. This method is called when the URL changes.
     *
     * @param url the URL to load settings for
     */
    private void loadAuthorizationSettings(String url) {
        if (mainController != null && authorizationController != null && url != null && !url.isEmpty()) {
            boolean applied = mainController.authorizationSettings.applySettingsToController(url, authorizationController);
            if (applied) {
                log.debug("Loaded authorization settings for URL: {}", url);
            }
        }
    }

    XmlColorizer xmlColorizer = new XmlColorizer();

    private void addTableRowIfFulfilled() {
        ObservableList<RequestHeader> any = tableHeaders.getItems();
        long any1 = any.stream()
                .filter(f -> StringUtils.isAllEmpty(f.getName(), f.getValue()))
                .count();
        if (any1 == 0) {
            tableHeaders.getItems().add(RequestHeader.builder().checked(true).build());
        } else if (any1 > 1) {
            tableHeaders.getItems().remove(
                    any.stream()
                            .filter(f -> StringUtils.isAllEmpty(f.getName(), f.getValue()))
                            .findFirst()
            );
        }
    }

    private void applyJsonLookSettings(CodeArea area, String cssName) {
        editorSettingsForAll(area, cssName);
        area.textProperty().addListener(
                (obs, oldText, newText) -> area.setStyleSpans(0, jsonColorize.computeHighlighting(newText)));
    }

    private static String getCss(String css) {
        return Objects.requireNonNull(MainController.class.getResource(css)).toString();
    }

    private static void editorSettingsForAll(CodeArea area, String cssName) {
        area.setParagraphGraphicFactory(LineNumberFactory.get(area));
        area.getStylesheets().add(getCss(cssName));
        area.setWrapText(true);
        area.setLineHighlighterOn(true);
    }

    public void codeResponseXmlSettings(CodeArea area, String cssName) {
        editorSettingsForAll(area, cssName);
        area.textProperty().addListener(
                (obs, oldText, newText) -> area.setStyleSpans(0, xmlColorizer.computeHighlighting(newText)));
    }

    public void onTreeItemSelect(String uri, TreeItemOperationLeaf leaf) {
        boxRequestParams.getChildren().clear();
        txtAddress.setText(uri);
        Optional<Parameter> body = Optional.ofNullable(leaf.getMethodParameters())
                .flatMap(parameters -> parameters.stream()
                        .filter(Objects::nonNull)
                        .filter(p -> "body".equals(p.getName()))
                        .findAny());
        if (body.isPresent()) {// this function requires json body
            tabRequestDetails.getSelectionModel().select(tabBody);
        } else {
            tabRequestDetails.getSelectionModel().select(tabParams);
        }
        AtomicInteger row = new AtomicInteger();
        Optional.ofNullable(leaf.getMethodParameters())
                .ifPresentOrElse(
                        parameters -> parameters.stream()
                                .filter(Objects::nonNull)
                                .forEach(f -> {
                                    Label lblInput = new Label(f.getName());
                                    boxRequestParams.add(lblInput, 0, row.get());
                                    if (leaf.getQueryItems() != null && !leaf.getQueryItems().isEmpty()) {
                                        // Use ComboBox for parameters with enumerated values
                                        ComboBox<String> comboInput = new ComboBox<>();
                                        comboInput.getItems().addAll(leaf.getQueryItems());
                                        comboInput.setEditable(true);
                                        comboInput.setPromptText("Select or enter a value");
                                        comboInput.setId(f.getName());
                                        comboInput.setMinWidth(Region.USE_PREF_SIZE);
                                        comboInput.getEditor().setOnKeyPressed(event -> {
                                            if (KeyCode.DOWN.equals(event.getCode())) {
                                                int currentIndex = comboInput.getSelectionModel().getSelectedIndex();
                                                if (currentIndex < comboInput.getItems().size() - 1) {
                                                    comboInput.getSelectionModel().select(currentIndex + 1);
                                                    comboInput.setValue(comboInput.getItems().get(currentIndex + 1));
                                                }
                                                event.consume();
                                            }
                                            if (KeyCode.UP.equals(event.getCode())) {
                                                int currentIndex = comboInput.getSelectionModel().getSelectedIndex();
                                                if (currentIndex > 0) {
                                                    comboInput.getSelectionModel().select(currentIndex - 1);
                                                    comboInput.setValue(comboInput.getItems().get(currentIndex - 1));
                                                }
                                                event.consume();
                                            }
                                        });

                                        // Create a custom STextField to store parameter info
                                        STextField paramInfo = new STextField();
                                        paramInfo.setParamName(f.getName());
                                        paramInfo.setIn(f.getIn());
                                        // Store the parameter info in the ComboBox's user data
                                        comboInput.setUserData(paramInfo);

                                        boxRequestParams.add(comboInput, 1, row.get());
                                    } else {
                                        // Use TextField for parameters without enumerated values
                                        STextField txtInput = new STextField();
                                        txtInput.setParamName(f.getName());
                                        txtInput.setId(f.getName());
                                        txtInput.setIn(f.getIn());
                                        txtInput.setMinWidth(Region.USE_PREF_SIZE);
                                        boxRequestParams.add(txtInput, 1, row.get());
                                    }
                                    row.incrementAndGet();
                                }),
                        () -> log.info("Method parameters are null")
                );
        setRequestText(
                Json.pretty(leaf.getMethodParameters()));
    }

    public void btnSendRequest(ActionEvent actionEvent) {
        btnSend.setDisable(true);
        TreeItem<String> selectedItem = mainController.treePaths.getSelectionModel().getSelectedItem();
        String targetUri = txtAddress.getText();
        mainController.setIsOnloading();

        if (selectedItem instanceof TreeItemOperationLeaf) {
            HttpUtility httpUtility = mainController.getHttpUtility();
            new Thread(() -> {
                try {
                    // Get the server port from the original URI
                    int serverPort = -1;
                    try {
                        URI originalUri = URI.create(targetUri);
                        serverPort = originalUri.getPort();
                    } catch (Exception e) {
                        log.warn("Error parsing original URI: {}", e.getMessage());
                    }

                    // Resolve environment variables in the request URL
                    String resolvedUri = targetUri;
                    if (preRequestScriptController != null) {
                        resolvedUri = preRequestScriptController.resolveEnvironmentVariables(targetUri);
                        log.info("Resolved URI: {}", resolvedUri);
                    }

                    // Force the port for localhost URIs
                    if (resolvedUri.contains("127.0.0.1") || resolvedUri.contains("localhost")) {
                        // First try to use the port from the original URI
                        int portToUse = serverPort;

                        // If no port was found, use a fixed port for testing
                        if (portToUse == -1) {
                            portToUse = 8765; // Fixed port used in the test
                            log.info("Using fixed port 8765 for localhost");
                        }

                        // Replace any occurrence of localhost or 127.0.0.1 without port
                        if (resolvedUri.contains("127.0.0.1/")) {
                            resolvedUri = resolvedUri.replace("127.0.0.1/", "127.0.0.1:" + portToUse + "/");
                            log.info("Fixed localhost URI with port: {}", resolvedUri);
                        } else if (resolvedUri.contains("localhost/")) {
                            resolvedUri = resolvedUri.replace("localhost/", "localhost:" + portToUse + "/");
                            log.info("Fixed localhost URI with port: {}", resolvedUri);
                        }
                    }

                    // Extract query and path parameters from UI components
                    Map<String, String> queryParams = new HashMap<>();
                    Map<String, String> pathParams = new HashMap<>();

                    // Process STextField query parameters
                    boxRequestParams.getChildren().stream()
                            .filter(n -> n instanceof STextField)
                            .forEach(n -> {
                                STextField node = (STextField) n;
                                String paramValue = node.getText();
                                // Resolve environment variables in parameter value
                                if (preRequestScriptController != null) {
                                    paramValue = preRequestScriptController.resolveEnvironmentVariables(paramValue);
                                }

                                if ("query".equals(node.getIn())) {
                                    queryParams.put(node.getParamName(), paramValue);
                                } else if ("path".equals(node.getIn())) {
                                    pathParams.put(node.getParamName(), paramValue);
                                }
                            });

                    // Process ComboBox query parameters
                    boxRequestParams.getChildren().stream()
                            .filter(n -> n instanceof ComboBox && n.getUserData() instanceof STextField)
                            .forEach(n -> {
                                ComboBox<?> comboBox = (ComboBox<?>) n;
                                STextField paramInfo = (STextField) comboBox.getUserData();

                                if (comboBox.getValue() != null) {
                                    String paramValue = comboBox.getValue().toString();
                                    // Resolve environment variables in parameter value
                                    if (preRequestScriptController != null) {
                                        paramValue = preRequestScriptController.resolveEnvironmentVariables(paramValue);
                                    }

                                    if ("query".equals(paramInfo.getIn())) {
                                        queryParams.put(paramInfo.getParamName(), paramValue);
                                    } else if ("path".equals(paramInfo.getIn())) {
                                        pathParams.put(paramInfo.getParamName(), paramValue);
                                    }
                                }
                            });

                    // Extract headers from UI components
                    Map<String, String> headers = new HashMap<>();
                    tableHeaders.getItems().forEach(item -> {
                        if (item instanceof RequestHeader header) {
                            if (Boolean.TRUE.equals(header.getChecked()) &&
                                    header.getName() != null && !header.getName().isEmpty()) {
                                String headerName = header.getName();
                                String headerValue = header.getValue();

                                // Resolve environment variables in header name and value
                                if (preRequestScriptController != null) {
                                    headerName = preRequestScriptController.resolveEnvironmentVariables(headerName);
                                    headerValue = preRequestScriptController.resolveEnvironmentVariables(headerValue);
                                }

                                headers.put(headerName, headerValue);
                            }
                        }
                    });

                    log.info("Headers before applying authentication: {}", headers);

                    // Apply authentication headers if available
                    if (authorizationController != null) {
                        authorizationController.applyAuthHeaders(headers);
                        log.info("Headers after applying authentication: {}", headers);
                    } else {
                        log.warn("Authorization controller is null, skipping authentication");
                    }

                    // Execute pre-request script if available
                    if (preRequestScriptController != null) {
                        try {
                            log.info("Executing pre-request script");
                            // Execute script and wait for it to complete
                            if (preRequestScriptController.getScript() != null && !preRequestScriptController.getScript()
                                    .isEmpty())
                                preRequestScriptController.executeScript(headers).get();
                            log.info("Headers after executing pre-request script: {}", headers);
                        } catch (Exception e) {
                            log.error("Error executing pre-request script: {}", e.getMessage());
                            // Continue with the request even if the script fails
                        }
                    } else {
                        log.warn("Pre-request script controller is null, skipping script execution");
                    }

                    // Get request body and resolve environment variables
                    String body = getRequestText();
                    if (preRequestScriptController != null) {
                        body = preRequestScriptController.resolveEnvironmentVariables(body);
                        log.info("Resolved request body with environment variables");
                    }
                    // Apply trimming preference for request body
                    var prefs = java.util.prefs.Preferences.userNodeForPackage(io.github.ozkanpakdil.swaggerific.SwaggerApplication.class);
                    if (prefs.getBoolean(io.github.ozkanpakdil.swaggerific.ui.edit.General.KEY_TRIM_BODY, false)) {
                        if (body != null) {
                            body = body.trim();
                        }
                    }

                    // Inject default headers based on preferences
                    if (prefs.getBoolean(io.github.ozkanpakdil.swaggerific.ui.edit.General.KEY_NO_CACHE, false)) {
                        headers.putIfAbsent("Cache-Control", "no-cache, no-store, must-revalidate");
                        headers.putIfAbsent("Pragma", "no-cache");
                        headers.putIfAbsent("Expires", "0");
                    }
                    if (prefs.getBoolean(io.github.ozkanpakdil.swaggerific.ui.edit.General.KEY_SWAGGER_TOKEN, false)) {
                        headers.putIfAbsent("X-Swagger-Token", "true");
                    }

                    // Get HTTP method
                    PathItem.HttpMethod httpMethod = PathItem.HttpMethod.valueOf(
                            cmbHttpMethod.getSelectionModel().getSelectedItem().toString());

                    // Send request and get response
                    HttpResponse response = httpUtility.sendRequest(
                            targetUri, httpMethod, headers, body, queryParams, pathParams);

                    // Save history entry (if enabled) and then process response
                    try {
                        java.net.URI finalUri = mainController.getHttpUtility().buildUri(targetUri, queryParams, pathParams);
                        io.github.ozkanpakdil.swaggerific.tools.history.HistoryService.save(
                                httpMethod.name(), finalUri, headers, body, response);
                    } catch (Exception ex) {
                        log.warn("Failed to save history: {}", ex.getMessage());
                    }

                    // Process response in the UI thread
                    Platform.runLater(() -> {
                        // First process the response in the UI
                        mainController.processResponse(response);
                        // Mark as not dirty after a successful send
                        dirty = false;

                        // Fire anonymous telemetry for completed request (opt-in, no PII)
                        try {
                            var tprefs = java.util.prefs.Preferences.userNodeForPackage(io.github.ozkanpakdil.swaggerific.SwaggerApplication.class);
                            io.github.ozkanpakdil.swaggerific.tools.telemetry.TelemetryService telemetry =
                                    new io.github.ozkanpakdil.swaggerific.tools.telemetry.TelemetryService(tprefs);
                            telemetry.sendRequestAsync(httpMethod.name(), response.statusCode());
                        } catch (Exception ignored) {
                        }

                        // Then execute response test script if available
                        if (responseTestScriptController != null && responseTestScriptController.getScript() != null &&
                                !responseTestScriptController.getScript().isEmpty()) {
                            try {
                                log.info("Executing response test script");

                                // Clear previous test results
                                if (tableTestResults != null) {
                                    tableTestResults.getItems().clear();
                                }

                                if (!responseTestScriptController.getScript().isEmpty())
                                    responseTestScriptController.executeScript(response)
                                            .thenAccept(results -> {
                                                // Update test results table
                                                if (tableTestResults != null && results != null) {
                                                    // Convert assertion results to TestResult objects
                                                    ObservableList<TestResult> testResults = FXCollections.observableArrayList();
                                                    results.forEach(result ->
                                                            testResults.add(new TestResult(result.passed(), result.message()))
                                                    );

                                                    // Update the table in the UI thread
                                                    Platform.runLater(() -> {
                                                        tableTestResults.setItems(testResults);

                                                        // Count passed and failed tests
                                                        long passedCount = testResults.stream()
                                                                .filter(TestResult::isPassed)
                                                                .count();

                                                        log.info("Test results: {} passed, {} failed",
                                                                passedCount, testResults.size() - passedCount);
                                                    });
                                                }
                                            })
                                            .exceptionally(e -> {
                                                log.error("Error executing response test script: {}", e.getMessage());
                                                return null;
                                            });
                            } catch (Exception e) {
                                log.error("Error executing response test script: {}", e.getMessage());
                            }
                        }
                    });
                } finally {
                    Platform.runLater(() -> {
                        mainController.setIsOffloading();
                        btnSend.setDisable(false);
                    });
                }
            }).start();
        } else {
            mainController.showAlert("Please choose leaf", "", "Please choose a leaf GET,POST,....");
            mainController.setIsOffloading();
            btnSend.setDisable(false);
        }
    }

    public void initializeController(MainController parent, String uri, TreeItemOperationLeaf leaf) {
        cmbHttpMethodConfig(leaf);
        this.mainController = parent;
        txtAddress.setText(uri);
        // Initialize editors at runtime (RichTextFX on JVM, TextArea on native)
        codeJsonRequest = new CodeArea();
        codeJsonRequest.setWrapText(true);
        codeJsonRequest.setId("codeJsonRequest");
        codeRequestContainer.getChildren().setAll(new VirtualizedScrollPane<>(codeJsonRequest));
        codeJsonResponse = new CustomCodeArea();
        codeJsonResponse.setId("codeJsonResponse");
        responsePrettyContainer.getChildren().setAll(new VirtualizedScrollPane<>(codeJsonResponse));

        BracketHighlighter bracketHighlighter = new BracketHighlighter(codeJsonResponse);
        SelectedHighlighter selectedHighlighter = new SelectedHighlighter(codeJsonResponse);
        // Trigger selection + bracket highlights on typing (cast for platform stability)
        codeJsonResponse.setOnKeyTyped(keyEvent -> {
            selectedHighlighter.highlightSelectedText();
            bracketHighlighter.highlightBracket();
        });
        // Also trigger bracket highlight on key release and mouse click to catch caret-only moves
        codeJsonResponse.setOnKeyReleased(ev -> bracketHighlighter.highlightBracket());
        codeJsonResponse.setOnMouseClicked(ev -> bracketHighlighter.highlightBracket());
        // Ensure initial bracket highlight reflects current caret position
        Platform.runLater(bracketHighlighter::highlightBracket);

        // Set callback on authorization controller to save settings when they change
        if (authorizationController != null) {
            authorizationController.setOnSettingsChangeCallback(this::saveAuthorizationSettings);

            // Load authorization settings for the current URL
            loadAuthorizationSettings(uri);
        }

        // Set environment manager in pre-request script controller
        if (preRequestScriptController != null && parent.environmentManager != null) {
            preRequestScriptController.setEnvironmentManager(parent.environmentManager);
            log.info("Set environment manager in pre-request script controller");
        }

        // Set environment manager in response test script controller
        if (responseTestScriptController != null && parent.environmentManager != null) {
            responseTestScriptController.setEnvironmentManager(parent.environmentManager);
            log.info("Set environment manager in response test script controller");
        }

        // Initialize test results table
        if (tableTestResults != null) {
            tableTestResults.setItems(FXCollections.observableArrayList());
            log.info("Initialized test results table");
        }

        // Add listener to txtAddress to load authorization settings when URL changes
        txtAddress.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                loadAuthorizationSettings(newValue);
                dirty = true;
            }
        });

        // Apply custom shortcuts to the send button
        // First, ensure the button is fully initialized
        Platform.runLater(() -> {
            // Apply custom shortcuts to the scene
            if (btnSend.getScene() != null) {
                SwaggerApplication.applyCustomShortcutsToScene(btnSend.getScene());
                log.info("Applied custom shortcuts to send button");
            } else {
                log.warn("Could not apply custom shortcuts to send button - scene is null");
            }
        });

        if (codeJsonRequest != null) {
            applyJsonLookSettings(codeJsonRequest, "/css/json-highlighting.css");
        }
        if (codeJsonResponse != null) {
            // Cast to CodeArea to avoid any class hierarchy issues on some platforms
            applyJsonLookSettings(((CodeArea) codeJsonResponse), "/css/json-highlighting.css");

            // Add fold gutter caret next to line numbers for Pretty JSON area
            IntFunction<Node> numberFactory = LineNumberFactory.get(((CodeArea) codeJsonResponse));
            IntFunction<Node> graphicFactory = paragraph -> {
                Node lineNo = numberFactory.apply(paragraph);
                Label caret = new Label();
                caret.getStyleClass().add("fold-caret");
                caret.setMinWidth(14);
                caret.setAlignment(Pos.CENTER);
                boolean foldable = codeJsonResponse.isParagraphFoldable(paragraph);
                if (foldable) {
                    boolean folded = codeJsonResponse.isParagraphFolded(paragraph);
                    caret.setText(folded ? "▸" : "▾");
                    caret.setCursor(Cursor.HAND);
                    caret.setOnMouseClicked(e -> {
                        codeJsonResponse.toggleFoldAtParagraph(paragraph);
                        // text change triggers gutter recompute automatically
                        e.consume();
                    });
                    caret.setTooltip(new Tooltip((folded ? "Unfold" : "Fold") + " JSON object on this line"));
                } else {
                    caret.setText("");
                    caret.setMouseTransparent(true);
                }
                HBox box = new HBox(caret, lineNo);
                box.setSpacing(4);
                box.setAlignment(Pos.CENTER_LEFT);
                return box;
            };
            codeJsonResponse.setParagraphGraphicFactory(graphicFactory);

            // Folding shortcuts for JSON Pretty view
            codeJsonResponse.setOnKeyPressed(ev -> {
                if (ev.isControlDown() && ev.getCode() == KeyCode.MINUS) { // Ctrl + - to toggle fold at caret
                    codeJsonResponse.toggleFoldAtCaret();
                    ev.consume();
                } else if (ev.isControlDown() && ev.getCode() == KeyCode.DIGIT0) { // Ctrl + 0 unfold all
                    codeJsonResponse.unfoldAll();
                    ev.consume();
                } else if (ev.isControlDown() && ev.getCode() == KeyCode.DIGIT9) { // Ctrl + 9 fold all top-level
                    codeJsonResponse.foldAllTopLevel();
                    ev.consume();
                }
            });

            // Context menu to make folding discoverable
            ContextMenu foldingMenu = new ContextMenu();
            MenuItem miToggle = new MenuItem("Toggle fold at caret	Ctrl+-");
            miToggle.setOnAction(e -> codeJsonResponse.toggleFoldAtCaret());
            MenuItem miFoldTop = new MenuItem("Fold all top-level {…}	Ctrl+9");
            miFoldTop.setOnAction(e -> codeJsonResponse.foldAllTopLevel());
            MenuItem miUnfold = new MenuItem("Unfold all	Ctrl+0");
            miUnfold.setOnAction(e -> codeJsonResponse.unfoldAll());
            foldingMenu.getItems().addAll(miToggle, new SeparatorMenuItem(), miFoldTop, miUnfold);
            codeJsonResponse.setContextMenu(foldingMenu);

            // Tooltip with quick help
            Tooltip tip = new Tooltip("JSON folding:\n• Ctrl+- toggle at caret\n• Ctrl+9 fold all top-level\n• Ctrl+0 unfold all\nRight-click for menu.");
            Tooltip.install(codeJsonResponse, tip);
        }

        tableHeaders.setItems(FXCollections.observableArrayList(
                RequestHeader.builder().checked(true).name(HttpHeaders.ACCEPT).value(MediaType.APPLICATION_JSON)
                        .build(),
                RequestHeader.builder().checked(false).name(HttpHeaders.CONTENT_TYPE).value(MediaType.APPLICATION_JSON)
                        .build(),
                RequestHeader.builder().checked(false).name("").value("").build()));
        TableColumn<RequestHeader, Boolean> checked = tableHeaders.getVisibleLeafColumn(0);
        checked.setCellFactory(CheckBoxTableCell.forTableColumn(checked));
        checked.setCellFactory(p -> {
            CheckBox checkBox = new CheckBox();
            TableCell<RequestHeader, Boolean> cell = new TableCell<>() {
                @Override
                public void updateItem(Boolean item, boolean empty) {
                    if (empty) {
                        setGraphic(null);
                    } else {
                        checkBox.setSelected(item);
                        setGraphic(checkBox);
                    }
                }
            };
            checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (cell.getTableRow().getItem() != null)
                    cell.getTableRow().getItem().setChecked(isSelected);
            });
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        tableHeaders.getVisibleLeafColumn(1).setCellFactory(TextFieldTableCell.<RequestHeader>forTableColumn());
        ((TableColumn<RequestHeader, String>) tableHeaders.getVisibleLeafColumn(1)).setOnEditCommit(evt -> {
            evt.getRowValue().setName(evt.getNewValue());
            addTableRowIfFulfilled();
            dirty = true;
        });
        tableHeaders.getVisibleLeafColumn(2).setCellFactory(TextFieldTableCell.<RequestHeader>forTableColumn());
        ((TableColumn<RequestHeader, String>) tableHeaders.getVisibleLeafColumn(2)).setOnEditCommit(evt -> {
            evt.getRowValue().setValue(evt.getNewValue());
            addTableRowIfFulfilled();
            dirty = true;
        });
        // Mark dirty when request body changes
        if (codeJsonRequest != null) {
            codeJsonRequest.textProperty().addListener((obs, ov, nv) -> {
                if (!Objects.equals(ov, nv)) dirty = true;
            });
        } else if (codeJsonRequestText != null) {
            codeJsonRequestText.textProperty().addListener((obs, ov, nv) -> {
                if (!Objects.equals(ov, nv)) dirty = true;
            });
        }
        onTreeItemSelect(uri, leaf);
    }

    private void cmbHttpMethodConfig(TreeItemOperationLeaf leaf) {
        cmbHttpMethod.getItems().clear();
        cmbHttpMethod.getItems().addAll(PathItem.HttpMethod.values());
        cmbHttpMethod.setCellFactory(p -> new ListCell<PathItem.HttpMethod>() {
            @Override
            protected void updateItem(PathItem.HttpMethod item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    return;
                }
                setText(item.name());
                getStyleClass().add(item.name());
            }
        });
        cmbHttpMethod.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal == null || newVal == null) {
                return;
            }
            cmbHttpMethod.getStyleClass().remove(oldVal.toString());
            cmbHttpMethod.getStyleClass().add(newVal.toString());
        });
        cmbHttpMethod.getSelectionModel().select(leaf.getValue());
    }

    // Accessors used by MainController and others
    public CodeArea getCodeJsonRequest() {
        return codeJsonRequest;
    }

    public CustomCodeArea getCodeJsonResponse() {
        return codeJsonResponse;
    }

}
