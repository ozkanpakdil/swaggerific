package com.mascix.swaggerific.ui;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.mascix.swaggerific.DisableWindow;
import com.mascix.swaggerific.data.SwaggerModal;
import com.mascix.swaggerific.data.TreeItemSerialisationWrapper;
import com.mascix.swaggerific.tools.HttpUtility;
import com.mascix.swaggerific.ui.component.TextAreaAppender;
import com.mascix.swaggerific.ui.component.TreeItemOperatinLeaf;
import com.mascix.swaggerific.ui.edit.SettingsController;
import com.mascix.swaggerific.ui.exception.NotYetImplementedException;
import com.mascix.swaggerific.ui.textfx.CustomCodeArea;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.PathItem;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.StatusBar;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPosition;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.StreamSupport;

@Slf4j
@Data
public class MainController implements Initializable {

    //TODO this can go to Preferences.userNodeForPackage in the future
    public static final String SESSION = "session.bin";
    public TabPane tabRequests;
    @FXML
    VBox mainBox;
    @FXML
    TreeView treePaths;
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
    DockNode storedDockNode;

    SwaggerModal jsonModal;
    JsonNode jsonRoot;
    TreeItem<String> treeItemRoot = new TreeItem<>("base root");
    String urlTarget;
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("loader.fxml"));
    VBox boxLoader;
    HttpUtility httpUtility = new HttpUtility();

    @SneakyThrows
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        treePaths.getSelectionModel()
                .selectedItemProperty()
                .addListener((ChangeListener<TreeItem<String>>) (observable, oldValue, newValue) -> {
                    onTreeItemSelect(newValue);
                });
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

        configureLoggerTextBox();

        mainNode.setDockPane(dockPaneMain);
        debugDockNode.setDockPane(dockPaneMain);
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

    @SneakyThrows
    private void handleTreeViewItemClick(String tabName, TreeItemOperatinLeaf leaf) {
        if (tabRequests.getTabs().stream().filter(f -> f.getId().equals(tabName)).findAny().isEmpty()) {
            FXMLLoader tab = new FXMLLoader(getClass().getResource("/com/mascix/swaggerific/tab-request.fxml"));
            Tab newTab = new Tab(tabName);
            newTab.setContent(tab.load());
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

    @SneakyThrows
    private void onTreeItemSelect(TreeItem<String> newValue) {
        if (newValue instanceof TreeItemOperatinLeaf m) {
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

    public void handleAboutAction(ActionEvent actionEvent) {
        showAlert("About Swaggerific 0.1.0",
                "Swaggerific 0.1.0",
                "This application is currently in development. Please use with caution." +
                        "Used technology stack: Java 17, JavaFX, Jackson, Swagger, Logback, DockFX, RichTextFX, ControlsFX, Lombok, Maven, Git, IntelliJ IDEA, GraalVM"

        );
    }

    public void menuFileExit(ActionEvent actionEvent) {
        Platform.exit();
    }

    @DisableWindow
    public void menuFileOpenSwagger(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog("https://petstore.swagger.io/v2/swagger.json");
        dialog.initOwner(mainBox.getScene().getWindow());
        dialog.setTitle("Enter swagger url");
        dialog.setContentText("URL:");
        dialog.setHeaderText("Enter the json url of swagger url.");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(urlSwaggerJson -> {
            Platform.runLater(() -> setIsOnloading());
            Runnable task = () -> openSwaggerUrl(urlSwaggerJson);
            task.run();
        });
    }

    @SneakyThrows
    void setIsOnloading() {
        statusBar.setText("Loading...");
        ProgressIndicator pi = new ProgressIndicator();
        boxLoader = new VBox(pi);
        boxLoader.setAlignment(Pos.CENTER);
        // Grey Background
        mainBox.setVisible(false);
        topPane.getChildren().add(0, boxLoader);
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

    @SneakyThrows
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

    @SneakyThrows
    private void openSwaggerUrl(String urlSwagger) {
        treeItemRoot.getChildren().clear();
        if (urlSwagger.matches(".*(swagger\\.json|openapi\\.json)$")) {
            URL urlApi = new URL(urlSwagger);
            urlTarget = urlSwagger.replaceAll("(swagger.json|openapi.json)$", "");
            treePaths.setRoot(treeItemRoot);
            try {
                jsonRoot = Json.mapper().readTree(urlApi);
                jsonModal = Json.mapper().readValue(urlApi, SwaggerModal.class);
                jsonModal.getTags().forEach(it -> {
                    TreeItem<String> tag = new TreeItem<>();
                    tag.setValue(it.getName());
                    jsonModal.getPaths().forEach((it2, pathItem) -> {
                        if (it2.contains(it.getName())) {
                            TreeItem path = new TreeItem();
                            path.setValue(it2);
                            tag.getChildren().add(path);
                            returnTreeItemsForTheMethod(pathItem, path.getChildren(), urlApi, jsonModal, it2);
                        }
                    });
                    treeItemRoot.getChildren().add(tag);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            showAlert("Error", "Invalid URL", "Please enter a valid URL ending with swagger.json or openapi.json");

        }
        treePaths.setShowRoot(false);
        Platform.runLater(this::setIsOffloading);
    }

    @SneakyThrows
    private void returnTreeItemsForTheMethod(PathItem pathItem, ObservableList<TreeItem<String>> children,
                                             URL urlSwagger, SwaggerModal jsonModal, String parentVal) {
        pathItem.readOperationsMap().forEach((k, v) -> {
            TreeItemOperatinLeaf it = TreeItemOperatinLeaf.builder()
                    .uri(urlTarget + parentVal.substring(1))
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

    public void treeOnClick(MouseEvent mouseEvent) {
        if (treeItemRoot.getChildren().size() == 0)// if no item there open swagger.json loader
            menuFileOpenSwagger(null);
    }

    public void onClose() {
        try {
            FileOutputStream out = new FileOutputStream(SESSION);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(new TreeItemSerialisationWrapper(treeItemRoot));
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
                log.error("Problem deserializing", e);
            }
        }
    }

    @SneakyThrows
    public void openSettings(ActionEvent actionEvent) {
        FXMLLoader settingsFxmlLoader = new FXMLLoader(getClass().getResource("/com/mascix/swaggerific/edit/settings.fxml"));
        Parent root = settingsFxmlLoader.load();
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

    public TableView<RequestHeader> getTableHeaders() {
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

    public void onChangeTrimConfig(ActionEvent actionEvent) {
        throw new NotYetImplementedException("Trim config changed");
    }

    @FXML
    public void openDebugConsole() {
        if (debugDockNode != null && debugDockNode.isVisible()) {
            storedDockNode = debugDockNode;
            debugDockNode.setVisible(!debugDockNode.isVisible());
            dockPaneMain.undock(debugDockNode);
        } else if (storedDockNode != null) {
            debugDockNode = storedDockNode;
            debugDockNode.setVisible(true);
            dockPaneMain.dock(debugDockNode, DockPosition.BOTTOM);
            storedDockNode = null;
        }

    }
}