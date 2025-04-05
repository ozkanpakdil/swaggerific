package io.github.ozkanpakdil.swaggerific.ui;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.ozkanpakdil.swaggerific.DisableWindow;
import io.github.ozkanpakdil.swaggerific.data.SwaggerModal;
import io.github.ozkanpakdil.swaggerific.data.TreeItemSerialisationWrapper;
import io.github.ozkanpakdil.swaggerific.tools.HttpUtility;
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
import java.util.ResourceBundle;
import java.util.stream.StreamSupport;

public class MainController implements Initializable {
    private static final Logger log = (Logger) LoggerFactory.getLogger(MainController.class);

    //TODO this can go to Preferences.userNodeForPackage in the future
    final String SESSION = System.getProperty("user.home") + "/.swaggerific/session.bin";

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
            // UI aligning
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
            controller.setMainController(this, tabName, leaf);
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
        showAlert("About Swaggerific 0.1.0",
                "Swaggerific 0.1.0",
                "This application is currently in development. Please use with caution." +
                        "Used technology stack: Java 17, JavaFX, Jackson, Swagger, Logback, DockFX, RichTextFX, ControlsFX, Maven, Git, IntelliJ IDEA, GraalVM"

        );
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
            try {
                Platform.runLater(this::setIsOnloading);
                openSwaggerUrl(urlSwaggerJson);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                Platform.runLater(this::setIsOffloading);
            }
        });
    }

    void setIsOnloading() {
        statusBar.setText("Loading...");
        ProgressIndicator pi = new ProgressIndicator();
        boxLoader = new VBox(pi);
        boxLoader.setAlignment(Pos.CENTER);
        // Grey Background
        mainBox.setVisible(false);
        topPane.getChildren().addFirst(boxLoader);
        // Thread.sleep(100);
        // children = mainBox.getChildren();
        // Platform.runLater(()->{mainBox.getChildren().setAll(loader);});
        // mainBox.getChildren().setAll(loader);
        // mainBox.setDisable(true);
        // mainBox.setManaged(false);
        // mainBox.setVisible(false);
        // loader.setVisible(true);
        // loader.setViewOrder(1);
        // mainBox.getChildren().clear();
        // mainBox.getChildren().add(loader);
    }

    void setIsOffloading() {
        statusBar.setText("Ok");
        topPane.getChildren().remove(boxLoader);
        mainBox.setVisible(true);
        // Platform.runLater(()->{mainBox.getChildren().setAll(children);});
        // mainBox.getChildren().setAll(children);
        // loader.setViewOrder(0);
        // loader.setVisible(false);
        // mainBox.setDisable(false);
        // mainBox.setManaged(true);
        // mainBox.setViewOrder(1);
    }

    private void openSwaggerUrl(String urlSwagger) throws Exception {
        treeFilter = new TreeFilter();
        txtFilterTree.setText("");
        treeItemRoot.getChildren().clear();
        URL urlApi = new URI(urlSwagger).toURL();
        treePaths.setRoot(treeItemRoot);
        try {
            jsonRoot = Json.mapper().readTree(urlApi);
            jsonModal = Json.mapper().readValue(urlApi, SwaggerModal.class);

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        treePaths.setShowRoot(false);
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
            treeFilter.filterTreeItems(treePaths.getRoot(), "");
            File sessionFile = new File(SESSION);
            sessionFile.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(sessionFile);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(new TreeItemSerialisationWrapper<>(treeItemRoot));
            oos.flush();
        } catch (Exception e) {
            log.error("Problem serializing", e);
        }
    }

    public void onOpening() {
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
    }

    public void openSettings(ActionEvent ignoredActionEvent) {
        FXMLLoader settingsFxmlLoader = new FXMLLoader(getClass().getResource(
                "/io/github/ozkanpakdil/swaggerific/edit/settings.fxml"));
        Parent root = null;
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
        controller.setMainWindow(this);
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
}