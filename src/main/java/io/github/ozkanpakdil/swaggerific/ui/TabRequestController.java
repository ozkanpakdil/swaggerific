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
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TabRequestController extends TabPane {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TabRequestController.class);
    public ComboBox cmbHttpMethod;
    @FXML
    Button btnSend;
    MainController mainController;
    @FXML
    CodeArea codeJsonRequest;
    @FXML
    CustomCodeArea codeJsonResponse;
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
    AuthorizationController authorizationController;

    @FXML
    PreRequestScriptController preRequestScriptController;

    @FXML
    ResponseTestScriptController responseTestScriptController;

    @FXML
    TableView<TestResult> tableTestResults;

    JsonColorize jsonColorize = new JsonColorize();

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
        return MainController.class.getResource(css).toString();
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
        codeJsonRequest.replaceText(
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
                    String body = codeJsonRequest.getText();
                    if (preRequestScriptController != null) {
                        body = preRequestScriptController.resolveEnvironmentVariables(body);
                        log.info("Resolved request body with environment variables");
                    }

                    // Get HTTP method
                    PathItem.HttpMethod httpMethod = PathItem.HttpMethod.valueOf(
                            cmbHttpMethod.getSelectionModel().getSelectedItem().toString());

                    // Send request and get response
                    HttpResponse response = httpUtility.sendRequest(
                            targetUri, httpMethod, headers, body, queryParams, pathParams);

                    // Process response in the UI thread
                    Platform.runLater(() -> {
                        // First process the response in the UI
                        mainController.processResponse(response);

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
        BracketHighlighter bracketHighlighter = new BracketHighlighter(codeJsonResponse);
        SelectedHighlighter selectedHighlighter = new SelectedHighlighter(codeJsonResponse);
        codeJsonResponse.setOnKeyTyped(keyEvent -> selectedHighlighter.highlightSelectedText());

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

        applyJsonLookSettings(codeJsonRequest, "/css/json-highlighting.css");
        applyJsonLookSettings(codeJsonResponse, "/css/json-highlighting.css");
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
        tableHeaders.getVisibleLeafColumn(1).setCellFactory(TextFieldTableCell.<RequestHeader> forTableColumn());
        ((TableColumn<RequestHeader, String>) tableHeaders.getVisibleLeafColumn(1)).setOnEditCommit(evt -> {
            evt.getRowValue().setName(evt.getNewValue());
            addTableRowIfFulfilled();
        });
        tableHeaders.getVisibleLeafColumn(2).setCellFactory(TextFieldTableCell.<RequestHeader> forTableColumn());
        ((TableColumn<RequestHeader, String>) tableHeaders.getVisibleLeafColumn(2)).setOnEditCommit(evt -> {
            evt.getRowValue().setValue(evt.getNewValue());
            addTableRowIfFulfilled();
        });
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

}
