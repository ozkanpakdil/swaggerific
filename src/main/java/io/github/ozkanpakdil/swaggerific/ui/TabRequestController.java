package io.github.ozkanpakdil.swaggerific.ui;

import io.github.ozkanpakdil.swaggerific.tools.HttpUtility;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TabRequestController extends TabPane {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TabRequestController.class);
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
                                    STextField txtInput = new STextField();
                                    txtInput.setParamName(f.getName());
                                    txtInput.setId(f.getName());
                                    txtInput.setIn(f.getIn());
                                    txtInput.setMinWidth(Region.USE_PREF_SIZE);
                                    if (leaf.getQueryItems() != null && !leaf.getQueryItems().isEmpty()) {
                                        // TODO instead of text field this should be dropdown || combobox || listview.
                                        txtInput.setPromptText(String.valueOf(leaf.getQueryItems()));
                                    }
                                    Label lblInput = new Label(f.getName());
                                    boxRequestParams.add(lblInput, 0, row.get());
                                    boxRequestParams.add(txtInput, 1, row.get());
                                    row.incrementAndGet();
                                }),
                        () -> log.info("Method parameters are null")
                );
        codeJsonRequest.replaceText(
                Json.pretty(leaf.getMethodParameters()));
    }

    public void btnSendRequest(ActionEvent actionEvent) {
        TreeItem<String> selectedItem = mainController.treePaths.getSelectionModel().getSelectedItem();
        String targetUri = txtAddress.getText();
        mainController.setIsOnloading();

        if (selectedItem instanceof TreeItemOperationLeaf) {
            HttpUtility httpUtility = mainController.getHttpUtility();
            Platform.runLater(() -> httpUtility.request(Json.mapper(), mainController, targetUri,
                    PathItem.HttpMethod.valueOf(selectedItem.getValue())));
        } else {
            mainController.showAlert("Please choose leaf", "", "Please choose a leaf GET,POST,....");
        }
        mainController.setIsOffloading();
    }

    public void setMainController(MainController parent, String uri, TreeItemOperationLeaf leaf) {
        this.mainController = parent;
        txtAddress.setText(uri);
        BracketHighlighter bracketHighlighter = new BracketHighlighter(codeJsonResponse);
        codeJsonResponse.setOnKeyTyped(keyEvent -> {
            /*
            //TODO this bock may be used in json request in the future
            String character = keyEvent.getCharacter();
            if (character.equals("[")) {
                int position = codeJsonResponse.getCaretPosition();
                codeJsonResponse.insert(position, "]", "loop");
                codeJsonResponse.moveTo(position);
            } else if (character.equals("]")) {
                int position = codeJsonResponse.getCaretPosition();
                if (position != codeJsonResponse.getLength()) {
                    String nextChar = codeJsonResponse.getText(position, position + 1);
                    if (nextChar.equals("]")) codeJsonResponse.deleteText(position, position + 1);
                }
            }*/

            //            bracketHighlighter.highlightBracket();
        });

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

    public MainController getMainController() {
        return mainController;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public CodeArea getCodeJsonRequest() {
        return codeJsonRequest;
    }

    public void setCodeJsonRequest(CodeArea codeJsonRequest) {
        this.codeJsonRequest = codeJsonRequest;
    }

    public CustomCodeArea getCodeJsonResponse() {
        return codeJsonResponse;
    }

    public void setCodeJsonResponse(CustomCodeArea codeJsonResponse) {
        this.codeJsonResponse = codeJsonResponse;
    }

    public TextArea getCodeRawJsonResponse() {
        return codeRawJsonResponse;
    }

    public void setCodeRawJsonResponse(TextArea codeRawJsonResponse) {
        this.codeRawJsonResponse = codeRawJsonResponse;
    }

    public TextField getTxtAddress() {
        return txtAddress;
    }

    public void setTxtAddress(TextField txtAddress) {
        this.txtAddress = txtAddress;
    }

    public GridPane getBoxRequestParams() {
        return boxRequestParams;
    }

    public void setBoxRequestParams(GridPane boxRequestParams) {
        this.boxRequestParams = boxRequestParams;
    }

    public VBox getBoxParams() {
        return boxParams;
    }

    public void setBoxParams(VBox boxParams) {
        this.boxParams = boxParams;
    }

    public TabPane getTabRequestDetails() {
        return tabRequestDetails;
    }

    public void setTabRequestDetails(TabPane tabRequestDetails) {
        this.tabRequestDetails = tabRequestDetails;
    }

    public Tab getTabBody() {
        return tabBody;
    }

    public void setTabBody(Tab tabBody) {
        this.tabBody = tabBody;
    }

    public Tab getTabParams() {
        return tabParams;
    }

    public void setTabParams(Tab tabParams) {
        this.tabParams = tabParams;
    }

    public TableView getTableHeaders() {
        return tableHeaders;
    }

    public void setTableHeaders(TableView tableHeaders) {
        this.tableHeaders = tableHeaders;
    }

    public JsonColorize getJsonColorize() {
        return jsonColorize;
    }

    public void setJsonColorize(JsonColorize jsonColorize) {
        this.jsonColorize = jsonColorize;
    }

    public XmlColorizer getXmlColorizer() {
        return xmlColorizer;
    }

    public void setXmlColorizer(XmlColorizer xmlColorizer) {
        this.xmlColorizer = xmlColorizer;
    }
}
