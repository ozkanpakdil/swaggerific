package com.mascix.swaggerific;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.PathItem;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;
import org.controlsfx.control.StatusBar;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController implements Initializable {

    public Button btnSend;
    @FXML
    VBox mainBox;
    @FXML
    CodeArea txtJson;
    @FXML
    TreeView treePaths;
    @FXML
    AnchorPane ancText;
    @FXML
    StackPane topPane;
    @FXML
    StatusBar statusBar;
    @FXML
    TextField txtAddress;

    SwaggerModal jsonModal;
    ObjectMapper mapper = new ObjectMapper();

    TreeItem<String> root = new TreeItem<>("base root");
    ObservableList<Node> children;
    String urlTarget;

    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("loader.fxml"));
    private Pane loader;
    private VBox boxLoader;

    private static final Pattern JSON_REGEX = Pattern.compile("(?<JSONCURLY>\\{|\\})|" +
            "(?<JSONPROPERTY>\\\".*\\\")\\s*:\\s*|" +
            "(?<JSONVALUE>\\\".*\\\")|" +
            "\\[(?<JSONARRAY>.*)\\]|" +
            "(?<JSONNUMBER>\\d+.?\\d*)|" +
            "(?<JSONBOOL>true|false)|" +
            "(?<JSONNULL>null)" +
            "(?<TEXT>.*)");
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = JSON_REGEX.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass
                    = matcher.group("JSONPROPERTY") != null ? "json_property"
                    : matcher.group("JSONARRAY") != null ? "json_array"
                    : matcher.group("JSONCURLY") != null ? "json_curly"
                    : matcher.group("JSONBOOL") != null ? "json_bool"
                    : matcher.group("JSONNULL") != null ? "json_null"
                    : matcher.group("JSONNUMBER") != null ? "json_number"
                    : matcher.group("JSONVALUE") != null ? "json_value"
                    : matcher.group("TEXT") != null ? "text"
                    : null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
    @SneakyThrows
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loader = fxmlLoader.load();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        txtJson.getStylesheets().add(this.getClass().getResource( "/css/json-highlighting-dark.css").toString());
        txtJson.setWrapText(true);
        txtJson.textProperty().addListener((obs, oldText, newText) ->
                txtJson.setStyleSpans(0, computeHighlighting(newText)));
        treePaths.getSelectionModel()
                .selectedItemProperty()
                .addListener((ChangeListener<TreeItem<String>>) (observable, oldValue, newValue) -> {
                    // newValue represents the selected itemTree
                    if (newValue instanceof MyTreeItem) { // if leaf
                        MyTreeItem m = (MyTreeItem) newValue;
                        try {
                            PathItem p = m.getBindPathItem();
                            txtAddress.setText(urlTarget + m.getParent().getValue().substring(1));
                            txtJson.replaceText(
                                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(p)
                            );
                            System.out.println("new:" + newValue.getValue());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
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
        showAlert("About", "Learning JavaFX with GraalVM", "Not ready for prod use nor anything :)");
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
    private void setIsOnloading() {
        statusBar.setText("Loading...");
        ProgressIndicator pi = new ProgressIndicator();
        boxLoader = new VBox(pi);
        boxLoader.setAlignment(Pos.CENTER);
        // Grey Background
        mainBox.setVisible(false);
        topPane.getChildren().add(0, boxLoader);
//        Thread.sleep(100);
//        children = mainBox.getChildren();
//        Platform.runLater(()->{mainBox.getChildren().setAll(loader);});
//        mainBox.getChildren().setAll(loader);
//        mainBox.setDisable(true);
//        mainBox.setManaged(false);
//        mainBox.setVisible(false);
//        loader.setVisible(true);
//        loader.setViewOrder(1);
//        mainBox.getChildren().clear();
//        mainBox.getChildren().add(loader);
    }

    @SneakyThrows
    private void setIsOffloading() {
        statusBar.setText("Ok");
        topPane.getChildren().remove(boxLoader);
        mainBox.setVisible(true);
//        Platform.runLater(()->{mainBox.getChildren().setAll(children);});
//        mainBox.getChildren().setAll(children);
//        loader.setViewOrder(0);
//        loader.setVisible(false);
//        mainBox.setDisable(false);
//        mainBox.setManaged(true);
//        mainBox.setViewOrder(1);
    }

    @SneakyThrows
    private void openSwaggerUrl(String urlSwagger) {
        root.getChildren().clear();
        URL urlApi = new URL(urlSwagger);
        urlTarget = urlSwagger.replace("swagger.json", "");
        treePaths.setRoot(root);
        try {
            jsonModal = mapper.readValue(urlApi, SwaggerModal.class);
            jsonModal.getTags().forEach(it -> {
                TreeItem<String> tag = new TreeItem<>();
                tag.setValue(it.getName());
                jsonModal.getPaths().forEach((it2, pathItem) -> {
                    if (it2.contains(it.getName())) {
                        TreeItem path = new TreeItem();
                        path.setValue(it2);
                        tag.getChildren().add(path);
                        returnTreeItemsForTheMethod(pathItem, path.getChildren(), urlApi);
                    }
                });
                root.getChildren().add(tag);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        treePaths.setShowRoot(false);
        Platform.runLater(() -> setIsOffloading());
    }


    private void returnTreeItemsForTheMethod(PathItem pathItem, ObservableList<TreeItem<String>> children, URL urlSwagger) {
        pathItem.readOperationsMap().forEach((k, v) -> {
            MyTreeItem it = new MyTreeItem();
            it.setValue(k.name());
            it.setBindPathItem(pathItem);
            children.add(it);
        });
    }

    public void treeOnClick(MouseEvent mouseEvent) {
        if (root.getChildren().size() == 0)
            menuFileOpenSwagger(null);
    }

    public void btnSendRequest(ActionEvent actionEvent) {
        showAlert("", "not implemented", "nope");
    }
}