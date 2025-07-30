package io.github.ozkanpakdil.swaggerific.ui;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.ozkanpakdil.swaggerific.DisableWindow;
import io.github.ozkanpakdil.swaggerific.data.AuthorizationSettings;
import io.github.ozkanpakdil.swaggerific.data.EnvironmentManager;
import io.github.ozkanpakdil.swaggerific.data.SwaggerModal;
import io.github.ozkanpakdil.swaggerific.data.TreeItemSerialisationWrapper;
import io.github.ozkanpakdil.swaggerific.tools.HttpUtility;
import io.github.ozkanpakdil.swaggerific.tools.ProxySettings;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.ui.component.TextAreaAppender;
import io.github.ozkanpakdil.swaggerific.ui.component.TreeFilter;
import io.github.ozkanpakdil.swaggerific.ui.component.TreeItemOperationLeaf;
import io.github.ozkanpakdil.swaggerific.ui.edit.SettingsController;
import io.github.ozkanpakdil.swaggerific.ui.exception.NotYetImplementedException;
import io.github.ozkanpakdil.swaggerific.ui.textfx.CustomCodeArea;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.PathItem;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.StatusBar;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPosition;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.StreamSupport;

public class MainController implements Initializable {
    private static final Logger log = (Logger) LoggerFactory.getLogger(MainController.class);

    //TODO this can go to Preferences.userNodeForPackage in the future
    public static final String APP_SETTINGS_HOME = System.getProperty("user.home") + "/.swaggerific";
    final String SESSION = APP_SETTINGS_HOME + "/session.bin";
    final String AUTH_SETTINGS = APP_SETTINGS_HOME + "/auth_settings.bin";

    public TabPane tabRequests;
    public TextField txtFilterTree;
    public AnchorPane treePane;
    public SplitPane treeSplit;
    private TreeFilter treeFilter = new TreeFilter();

    @FXML
    public MenuBar menuBar;
    @FXML
    VBox mainBox;
    @FXML
    TreeView<String> treePaths;
    @FXML
    StackPane topPane;
    @FXML
    StatusBar statusBar;
    @FXML
    TextArea console;
    @FXML
    DockNode mainNode;
    @FXML
    DockNode debugDockNode;
    @FXML
    DockPane dockPaneMain;
    @FXML
    MenuController menuBarController;
    DockNode storedDockNode;

    SwaggerModal jsonModal;
    JsonNode jsonRoot;
    TreeItem<String> treeItemRoot = new TreeItem<>("base root");
    String urlTarget;
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("loader.fxml"));
    VBox boxLoader;
    HttpUtility httpUtility = new HttpUtility();
    AuthorizationSettings authorizationSettings = new AuthorizationSettings();
    EnvironmentManager environmentManager = EnvironmentManager.loadSettings();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        menuBarController.setMainController(this);
        treePaths.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> onTreeItemSelect(newValue));
        treePaths.setCellFactory(treeView -> {
            final Label label = new Label();
            label.getStyleClass().add("highlight-on-hover");
            TreeCell<String> cell = new TreeCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(label);
                    }
                }
            };
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            cell.itemProperty().addListener((obs, oldItem, newItem) -> {
                label.getStyleClass().clear();
                label.getStyleClass().add("highlight-on-hover");
                if (newItem != null) {
                    label.setText(newItem);
                    Arrays.stream(PathItem.HttpMethod.values()).toList().forEach(httpMethodName -> {
                        if (newItem.equals(httpMethodName.name()) && label.getText().equals(newItem)) {
                            label.getStyleClass().add(newItem);
                            label.getStyleClass().add("myLeafLabel");
                            log.debug("labelclass:{},{},{}", newItem, label.getText(), httpMethodName);
                        }
                    });
                }
            });
            return cell;
        });
        Platform.runLater(() -> {
            treeSplit.setDividerPosition(0, 0.13);
            flipDebugConsole();
        });

        configureLoggerTextBox();

        mainNode.setDockPane(dockPaneMain);
        debugDockNode.setDockPane(dockPaneMain);
        // TODO below line did not work from main-view.css should be moved to CSS in the future
        debugDockNode.lookup(".dock-title-bar").setStyle("""
                -fx-background-color: #f4f4f4;
                """);
    }

    private void configureLoggerTextBox() {
        TextAreaAppender textAreaAppender = new TextAreaAppender(console);

        // Create an encoder and set its pattern
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        encoder.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        encoder.start();

        // Set the encoder in your appender
        textAreaAppender.setEncoder(encoder);
        textAreaAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        textAreaAppender.start();

        // Get the root logger and add your appender
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(textAreaAppender);
    }

    private void handleTreeViewItemClick(String tabName, TreeItemOperationLeaf leaf) {
        if (tabRequests.getTabs().stream().filter(f -> f.getId().equals(tabName)).findAny().isEmpty()) {
            FXMLLoader tab = new FXMLLoader(getClass().getResource("/io/github/ozkanpakdil/swaggerific/tab-request.fxml"));
            Tab newTab = new Tab(tabName);
            try {
                newTab.setContent(tab.load());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            TabRequestController controller = tab.getController();
            controller.initializeController(this, tabName, leaf);
            newTab.setUserData(controller);
            newTab.setId(tabName);
            tabRequests.getTabs().add(newTab);
            tabRequests.getSelectionModel().select(newTab);
        } else {
            tabRequests.getTabs().stream()
                    .filter(f -> f.getId().equals(tabName)).findAny()
                    .ifPresent(p -> tabRequests.getSelectionModel().select(p));
        }
    }

    private void onTreeItemSelect(TreeItem<String> newValue) {
        if (newValue instanceof TreeItemOperationLeaf m) {
            handleTreeViewItemClick(m.getUri(), m);
        }
    }

    public void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.initOwner(mainBox.getScene().getWindow());
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait().ifPresent(rs -> {
            if (rs == ButtonType.OK) {
                System.out.println("Pressed OK.");
            }
        });
    }

    public void handleAboutAction(ActionEvent ignoredActionEvent) {
        String version = getApplicationVersion();
        showAlert("About " + getApplicationName() + " " + version,
                getApplicationName() + " " + version,
                "This application is currently in development. Please use with caution.\n\n" +
                        "Technology Stack:\n" + getLibraryVersions()
        );
    }

    private Properties loadAppProperties() throws IOException {
        Properties properties = new Properties();
        try (var stream = getClass().getResourceAsStream("/application.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        }
        return properties;
    }

    private String getLibraryVersions() {
        try {
            Properties properties = loadAppProperties();
            return String.format("""
                            Java: %s
                            JavaFX: %s
                            Jackson: %s
                            Swagger: %s
                            Logback: %s
                            DockFX: %s
                            RichTextFX: %s
                            ControlsFX: %s
                            Maven: %s
                            GraalVM: %s""",
                    properties.getProperty("java.version", "unknown"),
                    properties.getProperty("javafx.version", "unknown"),
                    properties.getProperty("jackson.version", "unknown"),
                    properties.getProperty("swagger-core.version", "unknown"),
                    properties.getProperty("logback-classic.version", "unknown"),
                    properties.getProperty("dockfx.version", "unknown"),
                    properties.getProperty("richtextfx.version", "unknown"),
                    properties.getProperty("controlsfx.version", "unknown"),
                    properties.getProperty("maven.version", "unknown"),
                    properties.getProperty("graalvm.version", "unknown")
            );
        } catch (IOException e) {
            log.error("Could not load versions", e);
            return "Could not load version information";
        }
    }

    private String getApplicationVersion() {
        try {
            Properties properties = loadAppProperties();
            return properties.getProperty("project.version", "unknown");
        } catch (IOException e) {
            log.error("Could not load application.properties", e);
            return "unknown";
        }
    }

    private String getApplicationName() {
        try {
            Properties properties = loadAppProperties();
            return properties.getProperty("application.name", "Swaggerific");
        } catch (IOException e) {
            log.error("Could not load application.properties", e);
            return "Swaggerific";
        }
    }

    public void menuFileExit(ActionEvent ignoredActionEvent) {
        Platform.exit();
    }

    @DisableWindow
    public void menuFileOpenSwagger(ActionEvent ignoredActionEvent) {
        TextInputDialog dialog = new TextInputDialog("https://petstore.swagger.io/v2/swagger.json");
        dialog.initOwner(mainBox.getScene().getWindow());
        dialog.setTitle("Enter swagger url");
        dialog.setContentText("URL:");
        dialog.setHeaderText("Enter the json url of swagger url.");
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getEditor().setPrefWidth(400);
        dialog.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(urlSwaggerJson -> {
            setIsOnloading();
            new Thread(() -> {
                try {
                    openSwaggerUrl(urlSwaggerJson);
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        log.error("Error loading swagger URL", e);
                        showAlert("Error", "Failed to load Swagger URL", e.getMessage());
                    });
                } finally {
                    Platform.runLater(this::setIsOffloading);
                }
            }).start();
        });
    }

    void setIsOnloading() {
        statusBar.setText("Loading...");

        // Create a progress indicator with a fixed size
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMinSize(80, 80);
        pi.setMaxSize(80, 80);
        pi.setPrefSize(80, 80);
        pi.setVisible(true);

        // Create a VBox for the progress indicator
        boxLoader = new VBox(pi);
        boxLoader.setAlignment(Pos.CENTER);
        boxLoader.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);"); // Semi-transparent background
        boxLoader.setVisible(true);

        // Make the overlay cover the entire area
        boxLoader.prefWidthProperty().bind(topPane.widthProperty());
        boxLoader.prefHeightProperty().bind(topPane.heightProperty());
        boxLoader.setMinSize(100, 100);
        boxLoader.setMouseTransparent(false); // Make sure it can receive mouse events

        // Remove any existing boxLoader to avoid duplicates
        topPane.getChildren().removeIf(node -> node == boxLoader);

        // Add the overlay on top of everything
        topPane.getChildren().add(boxLoader);

        // Ensure the boxLoader is visible
        boxLoader.toFront();
    }

    void setIsOffloading() {
        statusBar.setText("Ready");

        // Remove the boxLoader from the topPane
        if (boxLoader != null) {
            topPane.getChildren().remove(boxLoader);
            boxLoader = null; // Clear the reference to allow garbage collection
        }
    }

    /**
     * Enables detailed proxy debugging by setting system properties and configuring loggers. This should be called before any
     * proxy operations to get detailed logs.
     */
    public void enableProxyDebugging() {
        // Enable detailed proxy debugging in ProxySettings
        ProxySettings.enableProxyDebugLogs();

        // Set our own logger to DEBUG level
        log.setLevel(ch.qos.logback.classic.Level.DEBUG);

        log.info("Proxy debugging enabled - detailed logs will be shown in the console");

        // Log current proxy settings
        logProxySettings();
    }

    /**
     * Logs the current proxy settings to help diagnose proxy authentication issues. This method is safe to call as it doesn't
     * log sensitive information like passwords.
     */
    public void logProxySettings() {
        log.info("Current proxy settings:");
        log.info("Using system proxy: {}", ProxySettings.useSystemProxy());

        if (!ProxySettings.useSystemProxy()) {
            log.info("Proxy type: {}", ProxySettings.getProxyType());
            log.info("Proxy server: {}:{}", ProxySettings.getProxyServer(), ProxySettings.getProxyPort());
            log.info("Proxy authentication enabled: {}", ProxySettings.useProxyAuth());

            if (ProxySettings.useProxyAuth()) {
                log.info("Proxy username: {}", ProxySettings.getProxyAuthUsername());
            }

            log.info("Proxy bypass hosts: {}", ProxySettings.getProxyBypass());
            log.info("SSL validation disabled: {}", ProxySettings.disableSslValidation());
        }
    }

    private void openSwaggerUrl(String urlSwagger) throws Exception {
        treeFilter = new TreeFilter();
        txtFilterTree.setText("");
        treeItemRoot.getChildren().clear();

        log.info("Opening Swagger URL: {}", urlSwagger);

        if (log.isDebugEnabled())
            enableProxyDebugging();

        ProxySettings.setupSystemWideProxy();

        URL urlApi = new URI(urlSwagger)
                .toURL();
        treePaths.setRoot(treeItemRoot);

        try {
            if (urlApi.toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid or empty URL");
            }

            HttpResponse response = httpUtility.sendRequest(urlApi.toString(), PathItem.HttpMethod.GET);

            String jsonContent = response.body();
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                throw new RuntimeException("Received empty response from server");
            }

            jsonRoot = Json.mapper().readTree(jsonContent);
            jsonModal = Json.mapper().readValue(jsonContent, SwaggerModal.class);

            if (StringUtils.isAllBlank(jsonModal.getSwagger(), jsonModal.getOpenapi())) {
                throw new RuntimeException("Json is not recognized");
            }
            if (!StringUtils.isBlank(jsonModal.getSwagger())) { // swagger json
                urlTarget = urlApi.getProtocol() + "://" + urlApi.getHost() + jsonModal.getBasePath();

                jsonModal.getTags().forEach(tag1 -> {
                    TreeItem<String> tag = new TreeItem<>();
                    tag.setValue(tag1.getName());
                    jsonModal.getPaths().forEach((path1, pathItem) -> {
                        if (path1.contains(tag1.getName())) {
                            TreeItem<String> path = new TreeItem<>();
                            path.setValue(path1);
                            tag.getChildren().add(path);
                            returnTreeItemsForTheMethod(pathItem, path.getChildren(), path1);
                        }
                    });
                    treeItemRoot.getChildren().add(tag);
                });
            } else if (!jsonModal.getOpenapi().isEmpty()) { //open api latest json
                urlTarget = urlApi.getProtocol() + "://" + urlApi.getHost();
                jsonModal.getPaths().forEach((path, pathItem) -> {
                    String[] pathParts = (!path.startsWith("/")) ?
                            path.split("/") :
                            path.substring(1).split("/");
                    TreeItem<String> currentItem = treeItemRoot;

                    for (String part : pathParts) {
                        Optional<TreeItem<String>> existingItem = currentItem.getChildren().stream()
                                .filter(item -> part.equals(item.getValue()))
                                .findFirst();
                        if (existingItem.isPresent()) {
                            currentItem = existingItem.get();
                        } else {
                            TreeItem<String> newItem = new TreeItem<>(part);
                            currentItem.getChildren().add(newItem);
                            currentItem = newItem;
                        }
                    }

                    TreeItem<String> finalCurrentItem = currentItem;
                    pathItem.readOperationsMap().forEach((method, operation) -> {
                        TreeItemOperationLeaf operationLeaf = TreeItemOperationLeaf.builder()
                                .uri(urlTarget + path)
                                .methodParameters(operation.getParameters())
                                .build();
                        operationLeaf.setValue(method.name());
                        finalCurrentItem.getChildren().add(operationLeaf);
                    });
                });
            }
        } catch (java.io.IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("407")) {
                log.error("Proxy authentication failed: {}", e.getMessage());
                throw new RuntimeException("Proxy authentication failed. Please check your proxy username and password.", e);
            } else {
                log.error("I/O error while loading Swagger URL: {}", e.getMessage());
                throw new RuntimeException("I/O error: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Failed to load Swagger URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load Swagger URL: " + e.getMessage(), e);
        }
    }

    private void returnTreeItemsForTheMethod(PathItem pathItem, ObservableList<TreeItem<String>> children,
            String parentVal) {
        pathItem.readOperationsMap().forEach((k, v) -> {
            TreeItemOperationLeaf it = TreeItemOperationLeaf.builder()
                    .uri(urlTarget + "/" + parentVal.substring(1))
                    .methodParameters(v.getParameters())
                    .build();
            it.setValue(k.name());
            List<String> enumList = Optional.ofNullable(jsonRoot.path("paths")
                            .path(parentVal)
                            .path(k.name().toLowerCase())
                            .path("parameters"))
                    .filter(JsonNode::isArray)
                    .map(parametersNode -> parametersNode.get(0))
                    .map(itemsNode -> itemsNode.path("items").path("enum"))
                    .filter(JsonNode::isArray)
                    .map(enumNode -> StreamSupport.stream(enumNode.spliterator(), false)
                            .map(JsonNode::asText)
                            .toList())
                    .orElse(new ArrayList<>());

            it.setQueryItems(enumList);

            children.add(it);
        });
    }

    public void treeOnClick(MouseEvent ignoredMouseEvent) {
        if (treeItemRoot.getChildren().isEmpty())// if no item there open swagger.json loader
            menuFileOpenSwagger(null);
    }

    public void onClose() {
        try {
            if (treePaths.getRoot() == null) {
                return;
            }

            // Save tree structure
            treeFilter.filterTreeItems(treePaths.getRoot(), "");
            File sessionFile = new File(SESSION);
            sessionFile.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(sessionFile);
                    ObjectOutputStream oos = new ObjectOutputStream(out)) {
                oos.writeObject(new TreeItemSerialisationWrapper<>(treeItemRoot));
                oos.flush();
            }

            // Save authorization settings
            File authSettingsFile = new File(AUTH_SETTINGS);
            authSettingsFile.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(authSettingsFile);
                    ObjectOutputStream oos = new ObjectOutputStream(out)) {
                oos.writeObject(authorizationSettings);
                oos.flush();
                log.info("Saved authorization settings with {} entries", authorizationSettings.size());
            }

            // Save environment settings
            environmentManager.saveSettings();
            log.info("Saved environment settings with {} environments", environmentManager.size());
        } catch (Exception e) {
            log.error("Problem serializing", e);
        }
    }

    public void onOpening() {
        // Load tree structure
        if (Paths.get(SESSION).toFile().isFile()) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(Files.readAllBytes(Path.of(SESSION))))) {
                treeItemRoot = (TreeItem<String>) ois.readObject();
                treePaths.setRoot(treeItemRoot);
                treePaths.setShowRoot(false);
            } catch (Exception e) {
                log.error("Problem deserializing the last session", e);
                try {
                    Files.delete(Paths.get(SESSION));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        // Load authorization settings
        if (Paths.get(AUTH_SETTINGS).toFile().isFile()) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(Files.readAllBytes(Path.of(AUTH_SETTINGS))))) {
                authorizationSettings = (AuthorizationSettings) ois.readObject();
                log.info("Loaded authorization settings with {} entries", authorizationSettings.size());
            } catch (Exception e) {
                log.error("Problem deserializing authorization settings", e);
                try {
                    Files.delete(Paths.get(AUTH_SETTINGS));
                    authorizationSettings = new AuthorizationSettings();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public void openSettings(ActionEvent ignoredActionEvent) {
        FXMLLoader settingsFxmlLoader = new FXMLLoader(getClass().getResource(
                "/io/github/ozkanpakdil/swaggerific/edit/settings.fxml"));
        Parent root;
        try {
            root = settingsFxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SettingsController controller = settingsFxmlLoader.getController();
        Stage stage = new Stage();
        stage.initOwner(mainBox.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL); // make the settings window focused only.
        Scene settingsScene = new Scene(root);
        settingsScene.addEventHandler(KeyEvent.KEY_PRESSED, t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                settingsScene.getWindow().hide();
            }
        });
        stage.setScene(settingsScene);
        stage.show();

        //        stage.setOnHidden(e -> controller.onClose());
    }

    public GridPane getBoxRequestParams() {
        return getSelectedTab().boxRequestParams;
    }

    private TabRequestController getSelectedTab() {
        return (TabRequestController) tabRequests.getSelectionModel().getSelectedItem().getUserData();
    }

    public TableView getTableHeaders() {
        return getSelectedTab().tableHeaders;
    }

    public CodeArea getCodeJsonRequest() {
        return getSelectedTab().codeJsonRequest;
    }

    public CustomCodeArea getCodeJsonResponse() {
        return getSelectedTab().codeJsonResponse;
    }

    public TextArea getCodeRawJsonResponse() {
        return getSelectedTab().codeRawJsonResponse;
    }

    public void codeResponseXmlSettings(CustomCodeArea codeJsonResponse, String cssPath) {
        getSelectedTab().codeResponseXmlSettings(codeJsonResponse, cssPath);
    }

    public void onChangeTrimConfig(ActionEvent ignoredActionEvent) {
        throw new NotYetImplementedException("Trim config changed");
    }

    @FXML
    public void flipDebugConsole() {
        if (debugDockNode != null && debugDockNode.isVisible()) {
            storedDockNode = debugDockNode;
            debugDockNode.setVisible(!debugDockNode.isVisible());
            dockPaneMain.undock(debugDockNode);
        } else if (storedDockNode != null) {
            openDebugConsole();
        }
    }

    public void openDebugConsole() {
        debugDockNode = storedDockNode;
        debugDockNode.setVisible(true);
        dockPaneMain.dock(debugDockNode, DockPosition.BOTTOM);
        storedDockNode = null;
    }

    public void reportBugOrFeatureRequestFromHelpMenu(ActionEvent ignoredEvent) {
        try {
            Desktop.getDesktop().browse(URI.create("https://github.com/ozkanpakdil/swaggerific/issues/new/choose"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens the Environment Variables management window.
     *
     * @param ignoredActionEvent the action event (ignored)
     */
    public void openEnvironmentVariables(ActionEvent ignoredActionEvent) {
        FXMLLoader environmentsFxmlLoader = new FXMLLoader(getClass().getResource(
                "/io/github/ozkanpakdil/swaggerific/edit/environments.fxml"));
        Parent root;
        try {
            root = environmentsFxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        io.github.ozkanpakdil.swaggerific.ui.edit.EnvironmentController controller = environmentsFxmlLoader.getController();
        Stage stage = new Stage();
        stage.initOwner(mainBox.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL); // make the environments window focused only.
        stage.setTitle("Environment Variables");

        Scene environmentsScene = new Scene(root);
        environmentsScene.addEventHandler(KeyEvent.KEY_PRESSED, t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                environmentsScene.getWindow().hide();
            }
        });

        stage.setScene(environmentsScene);
        stage.show();
    }

    public void filterTree(KeyEvent ignoredKeyEvent) {
        treeFilter.filterTreeItems(treeItemRoot, txtFilterTree.getText());
    }

    public void showHideTree(ActionEvent ignoredActionEvent) {
        boolean isVisible = treePaths.isVisible();
        treePaths.setVisible(!isVisible);
        treePane.setVisible(!isVisible);
        treePane.setManaged(!isVisible);
        // TODO this is not working properly, after it hides the tree pane it still takes the space, investigate to make it all hidden
        if (!isVisible) {
            treeSplit.setDividerPosition(0, 0.0);
        } else {
            treeSplit.setDividerPositions(0.25);
        }
    }

    public void showHideFilter(ActionEvent ignoredActionEvent) {
        txtFilterTree.setVisible(!txtFilterTree.isVisible());
        txtFilterTree.setManaged(!txtFilterTree.isManaged());
    }

    public void showHideStatusBar(ActionEvent ignoredActionEvent) {
        statusBar.setVisible(!statusBar.isVisible());
    }

    public void expandAllTree(ActionEvent ignoredActionEvent) {
        if (treePaths.getRoot() != null)
            treeFilter.expandCollapseAll(treePaths.getRoot(), true);
    }

    public void collapseAllTree(ActionEvent ignoredActionEvent) {
        if (treePaths.getRoot() != null)
            treePaths
                    .getRoot()
                    .getChildren()
                    .forEach(node -> treeFilter.expandCollapseAll(node, false));
    }

    public TreeView<String> getTreePaths() {
        return treePaths;
    }

    public Node getTopPane() {
        return topPane;
    }

    public HttpUtility getHttpUtility() {
        return httpUtility;
    }

    /**
     * Processes the HTTP response and updates the UI.
     *
     * @param response the HTTP response
     */
    public void processResponse(HttpResponse response) {
        try {
            if (response.isError()) {
                log.warn("Error in HTTP response: {}", response.errorMessage());
                openDebugConsole();
                getCodeJsonResponse().replaceText("Error in request: " + response.errorMessage() +
                        "\n\nPlease check your request parameters and try again. If the problem persists, " +
                        "check the server status or network connection.");
                return;
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Empty response body received");
                getCodeJsonResponse().replaceText(
                        "The server returned an empty response with status code: " + response.statusCode() +
                                "\n\nThis might be expected for some operations, or it could indicate an issue with the request."
                );
            } else if (httpUtility.isJsonResponse(response)) {
                try {
                    String formattedJson = httpUtility.formatJson(responseBody);
                    getCodeJsonResponse().replaceText(formattedJson);
                    log.info("Successfully processed JSON response with status code: {}", response.statusCode());
                } catch (Exception e) {
                    log.warn("Failed to parse JSON response: {}", e.getMessage());
                    // If JSON parsing fails, show the raw response
                    final String errorMessage = "Warning: Could not format as JSON. Showing raw response:\n\n" + responseBody;
                    getCodeJsonResponse().replaceText(errorMessage);
                }
            } else if (httpUtility.isXmlResponse(response)) {
                log.info("Processing XML response with status code: {}", response.statusCode());
                try {
                    String formattedXml = httpUtility.formatXml(responseBody);
                    codeResponseXmlSettings(getCodeJsonResponse(), "/css/xml-highlighting.css");
                    getCodeJsonResponse().replaceText(formattedXml);
                } catch (Exception e) {
                    log.warn("Failed to format XML response: {}", e.getMessage());
                    // If XML formatting fails, show the raw response
                    final String errorMessage = "Warning: Could not format as XML. Showing raw response:\n\n" + responseBody;
                    getCodeJsonResponse().replaceText(errorMessage);
                }
            } else {
                // Fallback to raw response
                log.info("Processing raw response with status code: {}", response.statusCode());
                getCodeJsonResponse().replaceText(responseBody);
            }

            // Always set the raw response
            getCodeRawJsonResponse().setText(responseBody);

            log.info("Request completed with status code: {}", response.statusCode());

        } catch (Exception e) {
            log.error("Error processing response: {}", e.getMessage(), e);
            openDebugConsole();
            getCodeJsonResponse().replaceText(
                    "Error processing response: " + e.getMessage() +
                            "\n\nThis is an application error. Please report this issue with the steps to reproduce it."
            );
        }
    }
}
