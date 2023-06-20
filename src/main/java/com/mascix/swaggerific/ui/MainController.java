package com.mascix.swaggerific.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mascix.swaggerific.DisableWindow;
import com.mascix.swaggerific.data.SwaggerModal;
import com.mascix.swaggerific.data.TreeItemSerialisationWrapper;
import com.mascix.swaggerific.tools.HttpUtility;
import com.mascix.swaggerific.ui.component.STextField;
import com.mascix.swaggerific.ui.component.TreeItemOperatinLeaf;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.StatusBar;
import org.fxmisc.richtext.CodeArea;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Data
public class MainController implements Initializable {

    //TODO this can go to Preferences.userNodeForPackage in the future
    public static final String SESSION = "session.bin";
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
    static XmlColorizer xmlColorizer = new XmlColorizer();
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
        codeResponseJsonSettings(codeJsonRequest);
        codeResponseJsonSettings(codeJsonResponse);
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
            addTableRowIfAllfilled();
        });
        tableHeaders.getVisibleLeafColumn(2).setCellFactory(TextFieldTableCell.<RequestHeader>forTableColumn());
        ((TableColumn<RequestHeader, String>) tableHeaders.getVisibleLeafColumn(2)).setOnEditCommit(evt -> {
            evt.getRowValue().setValue(evt.getNewValue());
            addTableRowIfAllfilled();
        });
    }

    private void addTableRowIfAllfilled() {
        ObservableList<RequestHeader> any = tableHeaders.getItems();
        long any1 = any.stream().filter(f -> StringUtils.isAllEmpty(f.getName(), f.getValue())).count();
        if (any1 == 0) {
            tableHeaders.getItems().add(RequestHeader.builder().checked(true).build());
        } else if (any1 > 1) {
            tableHeaders.getItems().remove(
                    any.stream().filter(f -> StringUtils.isAllEmpty(f.getName(), f.getValue())).findFirst().get());
        }
    }

    private void codeResponseJsonSettings(CodeArea area) {
        area.getStylesheets().add(getCss("/css/json-highlighting.css"));
        area.setWrapText(true);
        area.textProperty().addListener(
                (obs, oldText, newText) -> area.setStyleSpans(0, jsonColorizer.computeHighlighting(newText)));
    }

    public static void codeResponseXmlSettings(CodeArea area) {
        area.getStylesheets().add(getCss("/css/xml-highlighting.css"));
        area.setWrapText(true);
        area.textProperty().addListener(
                (obs, oldText, newText) -> area.setStyleSpans(0, xmlColorizer.computeHighlighting(newText)));
    }

    @SneakyThrows
    private void onTreeItemSelect(TreeItem<String> newValue) {
        if (newValue instanceof TreeItemOperatinLeaf) {
            boxRequestParams.getChildren().clear();
            TreeItemOperatinLeaf m = (TreeItemOperatinLeaf) newValue;
            Optional<Parameter> body = m.getMethodParameters().stream().filter(p -> p.getName().equals("body")).findAny();
            if (!body.isEmpty()) {// this function requires json body
                tabRequestDetails.getSelectionModel().select(tabBody);
                return;
            } else {
                tabRequestDetails.getSelectionModel().select(tabParams);
            }
            txtAddress.setText(m.getUri() + m.getParent().getValue().toString().substring(1));
            AtomicInteger row = new AtomicInteger();
            m.getMethodParameters().forEach(f -> {
                STextField txtInput = new STextField();
                txtInput.setParamName(f.getName());
                txtInput.setIn(f.getIn());
                txtInput.setMinWidth(Region.USE_PREF_SIZE);
                if (m.getQueryItems() != null && m.getQueryItems().size() > 0) {
                    // TODO instead of text field this should be dropdown || combobox || listview.
                    txtInput.setPromptText(String.valueOf(m.getQueryItems()));
                }
                Label lblInput = new Label();
                lblInput.setText(f.getName());
                boxRequestParams.add(lblInput, 0, row.get());
                boxRequestParams.add(txtInput, 1, row.get());
                row.incrementAndGet();
            });
            codeJsonRequest.replaceText(
                    Json.pretty(m.getMethodParameters()));
        }
    }

    private static String getCss(String css) {
        return MainController.class.getResource(css).toString();
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
    private void setIsOffloading() {
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
        root.getChildren().clear();
        URL urlApi = new URL(urlSwagger);
        urlTarget = urlSwagger.replace("swagger.json", "");
        treePaths.setRoot(root);
        try {
            jsonRoot = mapper.readTree(urlApi);
            jsonModal = mapper.readValue(urlApi, SwaggerModal.class);
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
                            .collect(Collectors.toList()))
                    .orElse(new ArrayList<>());

            it.setQueryItems(enumList);

            children.add(it);
        });
    }

    public void treeOnClick(MouseEvent mouseEvent) {
        if (root.getChildren().size() == 0)// if no item there open swagger.json loader
            menuFileOpenSwagger(null);
    }

    public void btnSendRequest(ActionEvent actionEvent) {
        TreeItem<String> selectedItem = (TreeItem<String>) treePaths.getSelectionModel().getSelectedItem();

        if (selectedItem.getValue().equals("GET")) {
            Platform.runLater(() -> httpUtility.getRequest(mapper, this));
        } else if (selectedItem.getValue().equals("POST")) {
            Platform.runLater(() -> httpUtility.postRequest(codeJsonRequest, codeJsonResponse, txtAddress,
                    boxRequestParams, mapper, tableHeaders));
        } else {
            showAlert("", "", selectedItem.getValue() + " not implemented yet");
            log.error(selectedItem.getValue() + " not implemented yet");
        }
    }

    public void shutdown() {
        try {
            FileOutputStream out = new FileOutputStream(SESSION);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(new TreeItemSerialisationWrapper(root));
            oos.flush();
        } catch (Exception e) {
            log.error("Problem serializing", e);
        }
    }

    public void onOpening() {
        if (Paths.get(SESSION).toFile().isFile()) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(Files.readAllBytes(Path.of(SESSION))))) {
                root = (TreeItem<String>) ois.readObject();
                treePaths.setRoot(root);
                treePaths.setShowRoot(false);
            } catch (Exception e) {
                log.error("Problem deserializing", e);
            }
        }
    }
}