package com.mascix.swaggerific.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mascix.swaggerific.DisableWindow;
import com.mascix.swaggerific.data.SwaggerModal;
import com.mascix.swaggerific.tools.HttpUtility;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.StatusBar;
import org.fxmisc.richtext.CodeArea;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    CodeArea codeJsonResponse;
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

    SwaggerModal jsonModal;
    JsonNode jsonRoot;
    JsonColorizer jsonColorizer = new JsonColorizer();
    TreeItem<String> root = new TreeItem<>("base root");
    String urlTarget;
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("loader.fxml"));
    private VBox boxLoader;
    ObjectMapper mapper = new ObjectMapper();
    private HttpUtility httpUtility = new HttpUtility();

    @SneakyThrows
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        codeJsonRequest.getStylesheets().add(getCss("/css/json-highlighting.css"));
        codeJsonRequest.setWrapText(true);
        codeJsonRequest.textProperty().addListener((obs, oldText, newText) -> codeJsonRequest.setStyleSpans(0, jsonColorizer.computeHighlighting(newText)));
        codeJsonResponse.getStylesheets().add(getCss("/css/json-highlighting.css"));
        codeJsonResponse.setWrapText(true);
        codeJsonResponse.textProperty().addListener((obs, oldText, newText) -> codeJsonResponse.setStyleSpans(0, jsonColorizer.computeHighlighting(newText)));
        treePaths.getSelectionModel()
                .selectedItemProperty()
                .addListener((ChangeListener<TreeItem<String>>) (observable, oldValue, newValue) -> {
                    onTreeItemSelect(newValue);
                });
        tableHeaders.getVisibleLeafColumn(0).setCellFactory(TextFieldTableCell.<RequestHeader>forTableColumn());
        tableHeaders.getVisibleLeafColumn(1).setCellFactory(TextFieldTableCell.<RequestHeader>forTableColumn());
    }

    private void onTreeItemSelect(TreeItem<String> newValue) {
        if (newValue instanceof TreeItemOperatinLeaf) {
            boxRequestParams.getChildren().clear();
            TreeItemOperatinLeaf m = (TreeItemOperatinLeaf) newValue;
            Optional<Parameter> body = m.getParameters().stream().filter(p -> p.getName().equals("body")).findAny();
            if (!body.isEmpty()) {// this function requires json body
                tabRequestDetails.getSelectionModel().select(tabBody);
                return;
            } else {
                tabRequestDetails.getSelectionModel().select(tabParams);
            }
            txtAddress.setText(urlTarget + m.getParent().getValue().substring(1));
            AtomicInteger row = new AtomicInteger();
            m.getParameters().forEach(f -> {
                STextField txtInput = new STextField();
                txtInput.setParamName(f.getName());
                txtInput.setIn(f.getIn());
                txtInput.setMinWidth(Region.USE_PREF_SIZE);
                if (m.getItems() != null && m.getItems().size() > 0) {
//                   txtInput.setTextFormatter(new TextFormatter<>(new ExpectedTextFilter("Hello, World!")));
                    //TODO instead of text field this can be dropdown.
                    txtInput.setPromptText(String.valueOf(m.getItems()));
                }
                Label lblInput = new Label();
                lblInput.setText(f.getName());
                boxRequestParams.add(lblInput, 0, row.get());
                boxRequestParams.add(txtInput, 1, row.get());
                row.incrementAndGet();
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
                    }
            );
            return cell;
        });
        try {
            jsonRoot = mapper.readTree(urlApi);
            jsonModal = mapper.readValue(urlApi, SwaggerModal.class);
//            SwaggerParseResult result = new OpenAPIParser().readLocation("https://petstore3.swagger.io/api/v3/openapi.json", null, null);
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
                root.getChildren().add(tag);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        treePaths.setShowRoot(false);
        Platform.runLater(() -> setIsOffloading());
    }

    private void returnTreeItemsForTheMethod(PathItem pathItem, ObservableList<TreeItem<String>> children, URL urlSwagger, SwaggerModal jsonModal, String it2) {
        pathItem.readOperationsMap().forEach((k, v) -> {
            TreeItemOperatinLeaf it = new TreeItemOperatinLeaf();
            it.setValue(k.name());
            it.setParameters(v.getParameters());
            try {
                List<String> enumList = StreamSupport.stream(jsonRoot
                                .path("paths")
                                .path(it2)
                                .path(k.name().toLowerCase())
                                .path("parameters")
                                .get(0)
                                .path("items")
                                .path("enum")
                                .spliterator(), false)
                        .map(JsonNode::asText)
                        .collect(Collectors.toList());
                it.setItems(enumList);
            } catch (Exception e) {
// not nice, TODO fix this items extraction
            }

            children.add(it);
        });
    }

    public void treeOnClick(MouseEvent mouseEvent) {
        if (root.getChildren().size() == 0)
            menuFileOpenSwagger(null);
    }

    public void btnSendRequest(ActionEvent actionEvent) {
        TreeItem<String> selectedItem = (TreeItem<String>) treePaths.getSelectionModel().getSelectedItem();

        if (selectedItem.getValue().equals("GET")) {
            Platform.runLater(() -> httpUtility.getRequest(treePaths, codeJsonResponse, txtAddress, boxRequestParams, mapper,tableHeaders));
        } else if (selectedItem.getValue().equals("POST")) {
            Platform.runLater(() -> httpUtility.postRequest(codeJsonRequest, codeJsonResponse, txtAddress, boxRequestParams, mapper,tableHeaders));
        } else {
            showAlert("", "", selectedItem.getValue() + " not implemented yet");
            log.error(selectedItem.getValue() + " not implemented yet");
        }
    }


}