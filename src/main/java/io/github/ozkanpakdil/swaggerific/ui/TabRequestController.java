package io.github.ozkanpakdil.swaggerific.ui;

import io.github.ozkanpakdil.swaggerific.tools.HttpUtility;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.ui.component.STextField;
import io.github.ozkanpakdil.swaggerific.ui.component.TreeItemOperationLeaf;
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

    JsonColorize jsonColorize = new JsonColorize();
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
                    // Extract query and path parameters from UI components
                    Map<String, String> queryParams = new HashMap<>();
                    Map<String, String> pathParams = new HashMap<>();

                    // Process STextField query parameters
                    boxRequestParams.getChildren().stream()
                            .filter(n -> n instanceof STextField)
                            .forEach(n -> {
                                STextField node = (STextField) n;
                                if ("query".equals(node.getIn())) {
                                    queryParams.put(node.getParamName(), node.getText());
                                } else if ("path".equals(node.getIn())) {
                                    pathParams.put(node.getParamName(), node.getText());
                                }
                            });

                    // Process ComboBox query parameters
                    boxRequestParams.getChildren().stream()
                            .filter(n -> n instanceof ComboBox && n.getUserData() instanceof STextField)
                            .forEach(n -> {
                                ComboBox<?> comboBox = (ComboBox<?>) n;
                                STextField paramInfo = (STextField) comboBox.getUserData();

                                if (comboBox.getValue() != null) {
                                    if ("query".equals(paramInfo.getIn())) {
                                        queryParams.put(paramInfo.getParamName(), comboBox.getValue().toString());
                                    } else if ("path".equals(paramInfo.getIn())) {
                                        pathParams.put(paramInfo.getParamName(), comboBox.getValue().toString());
                                    }
                                }
                            });

                    // Extract headers from UI components
                    Map<String, String> headers = new HashMap<>();
                    tableHeaders.getItems().forEach(item -> {
                        if (item instanceof RequestHeader header) {
                            if (Boolean.TRUE.equals(header.getChecked()) && 
                                header.getName() != null && !header.getName().isEmpty()) {
                                headers.put(header.getName(), header.getValue());
                            }
                        }
                    });

                    // Get request body
                    String body = codeJsonRequest.getText();

                    // Get HTTP method
                    PathItem.HttpMethod httpMethod = PathItem.HttpMethod.valueOf(
                            cmbHttpMethod.getSelectionModel().getSelectedItem().toString());

                    // Send request and get response
                    HttpResponse response = httpUtility.sendRequest(
                            targetUri, httpMethod, headers, body, queryParams, pathParams);

                    // Process response in the UI thread
                    Platform.runLater(() -> mainController.processResponse(response));
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
