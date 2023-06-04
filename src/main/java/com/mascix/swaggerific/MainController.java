package com.mascix.swaggerific;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.StatusBar;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;
import java.util.regex.Matcher;

@Slf4j
public class MainController implements Initializable {

    public Button btnSend;
    @FXML
    VBox mainBox;
    @FXML
    CodeArea codeJsonRequest;
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
    @FXML
    CodeArea txtJsonResponse;
    @FXML
    VBox boxRequestParams;

    SwaggerModal jsonModal;
    ObjectMapper mapper = new ObjectMapper();

    TreeItem<String> root = new TreeItem<>("base root");
    ObservableList<Node> children;
    String urlTarget;

    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("loader.fxml"));
    private Pane loader;
    private VBox boxLoader;

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = JsonColorizer.JSON_REGEX.matcher(text);
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
        codeJsonRequest.getStylesheets().add(getCss("/css/json-highlighting.css"));
        txtJsonResponse.getStylesheets().add(getCss("/css/json-highlighting.css"));
        codeJsonRequest.setWrapText(true);
        txtJsonResponse.setWrapText(true);
        codeJsonRequest.textProperty().addListener((obs, oldText, newText) -> codeJsonRequest.setStyleSpans(0, computeHighlighting(newText)));
        txtJsonResponse.textProperty().addListener((obs, oldText, newText) -> txtJsonResponse.setStyleSpans(0, computeHighlighting(newText)));
        treePaths.getSelectionModel()
                .selectedItemProperty()
                .addListener((ChangeListener<TreeItem<String>>) (observable, oldValue, newValue) -> {
                    onTreeItemSelect(newValue);
                });
    }

    private void onTreeItemSelect(TreeItem<String> newValue) {
        if (newValue instanceof TreeItemOperatinLeaf) {
            boxRequestParams.getChildren().clear();
            TreeItemOperatinLeaf m = (TreeItemOperatinLeaf) newValue;
            txtAddress.setText(urlTarget + m.getParent().getValue().substring(1));

            m.getParameters().forEach(f -> {
                if (f.getRequired()) {
                    STextField txtInput = new STextField();
                    txtInput.setParamName(f.getName());
                    Label lblInput = new Label();
                    lblInput.setText(f.getName());
                    //TODO this hbox needs some padding and margin configuration but functionally working.
                    HBox l = new HBox();
                    l.getChildren().add(lblInput);
                    l.getChildren().add(txtInput);
                    boxRequestParams.getChildren().add(l);
                }
            });
            codeJsonRequest.replaceText(
                    Json.pretty(m.getParameters())
            );

        }
    }

    private String getCss(String css) {
        return this.getClass().getResource(css).toString();
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
                        if (newItem != null) {
                            label.setText(newItem);
                            Arrays.stream(PathItem.HttpMethod.values()).toList().forEach(it -> {
                                if (it.name().equals(newItem)) {
                                    label.getStyleClass().add(newItem);
                                    label.getStyleClass().add("myLeafLabel");
                                }
                            });
                        } else {
                            label.getStyleClass().clear();
                            label.getStyleClass().add("highlight-on-hover");
                        }
                    }
            );
            return cell;
        });
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
                        returnTreeItemsForTheMethod(pathItem, path.getChildren(), urlApi, jsonModal);
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

    private void returnTreeItemsForTheMethod(PathItem pathItem, ObservableList<TreeItem<String>> children, URL urlSwagger, SwaggerModal jsonModal) {
        pathItem.readOperationsMap().forEach((k, v) -> {
            TreeItemOperatinLeaf it = new TreeItemOperatinLeaf();
            it.setValue(k.name());
            it.setParameters(v.getParameters());
            children.add(it);
        });
    }

    public void treeOnClick(MouseEvent mouseEvent) {
        if (root.getChildren().size() == 0)
            menuFileOpenSwagger(null);
    }

    public void btnSendRequest(ActionEvent actionEvent) {
        TreeItem<String> selectedItem = (TreeItem<String>) treePaths.getSelectionModel().getSelectedItem();

        switch (selectedItem.getValue()) {
            case "GET":
                Platform.runLater(() -> getRequest());
            default:
                log.error(selectedItem.getValue() + " not implemented yet");
        }

    }

    @SneakyThrows
    private void getRequest() {
        TreeItemOperatinLeaf selectedItem = (TreeItemOperatinLeaf) treePaths.getSelectionModel().getSelectedItem();
        final String[] queryParams = {""};
        boxRequestParams.getChildren().stream().forEach(n -> {
            HBox h = (HBox) n;
            STextField node = (STextField) h.getChildren().get(1);
            queryParams[0] += node.getParamName() + "=" + node.getText() + "&";
        });
        URI uri = URI.create(txtAddress.getText() + "?" + queryParams[0]);
        log.info("uri:{}", uri);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();

        HttpResponse<String> httpResponse = client.send(request, BodyHandlers.ofString());

        txtJsonResponse.replaceText(
                Json.pretty(mapper.readTree(httpResponse.body()))
        );
    }
}