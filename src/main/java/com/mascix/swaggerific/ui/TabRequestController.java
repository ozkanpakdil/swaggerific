package com.mascix.swaggerific.ui;

import com.mascix.swaggerific.tools.HttpUtility;
import com.mascix.swaggerific.ui.component.STextField;
import com.mascix.swaggerific.ui.component.TreeItemOperatinLeaf;
import com.mascix.swaggerific.ui.textfx.*;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Data
public class TabRequestController extends TabPane {
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

    public void onTreeItemSelect(String uri, TreeItemOperatinLeaf leaf) {
        boxRequestParams.getChildren().clear();
        Optional<Parameter> body = leaf.getMethodParameters().stream().filter(p -> p.getName().equals("body")).findAny();
        txtAddress.setText(uri);
        if (body.isPresent()) {// this function requires json body
            tabRequestDetails.getSelectionModel().select(tabBody);
        } else {
            tabRequestDetails.getSelectionModel().select(tabParams);
        }
        AtomicInteger row = new AtomicInteger();
        leaf.getMethodParameters().forEach(f -> {
            STextField txtInput = new STextField();
            txtInput.setParamName(f.getName());
            txtInput.setId(f.getName());
            txtInput.setIn(f.getIn());
            txtInput.setMinWidth(Region.USE_PREF_SIZE);
            if (leaf.getQueryItems() != null && leaf.getQueryItems().size() > 0) {
                // TODO instead of text field this should be dropdown || combobox || listview.
                txtInput.setPromptText(String.valueOf(leaf.getQueryItems()));
            }
            Label lblInput = new Label();
            lblInput.setText(f.getName());
            boxRequestParams.add(lblInput, 0, row.get());
            boxRequestParams.add(txtInput, 1, row.get());
            row.incrementAndGet();
        });
        codeJsonRequest.replaceText(
                Json.pretty(leaf.getMethodParameters()));
    }

    public void btnSendRequest(ActionEvent actionEvent) {
        TreeItem<String> selectedItem = (TreeItem<String>) mainController.treePaths.getSelectionModel().getSelectedItem();
        TreeItemOperatinLeaf getSelectedItem = (TreeItemOperatinLeaf) mainController.getTreePaths().getSelectionModel().getSelectedItem();

        mainController.setIsOnloading();
        if (selectedItem instanceof TreeItemOperatinLeaf) {
            HttpUtility httpUtility = mainController.getHttpUtility();
            if (selectedItem.getValue().equals(PathItem.HttpMethod.GET.name())) {
                Platform.runLater(() -> httpUtility.getRequest(Json.mapper(), mainController));
            } else if (selectedItem.getValue().equals(PathItem.HttpMethod.POST.name())) {
                Platform.runLater(() -> httpUtility.postRequest(Json.mapper(), mainController, getSelectedItem.getUri()));
            } else if (selectedItem.getValue().equals(PathItem.HttpMethod.DELETE.name())) {
                Platform.runLater(() -> httpUtility.deleteRequest(Json.mapper(), mainController));
            } else if (selectedItem.getValue().equals(PathItem.HttpMethod.HEAD.name())) {
                Platform.runLater(() -> httpUtility.headRequest(Json.mapper(), mainController));
            } else if (selectedItem.getValue().equals(PathItem.HttpMethod.OPTIONS.name())) {
                Platform.runLater(() -> httpUtility.optionsRequest(Json.mapper(), mainController));
            } else if (selectedItem.getValue().equals(PathItem.HttpMethod.PATCH.name())) {
                Platform.runLater(() -> httpUtility.patchRequest(Json.mapper(), mainController));
            } else if (selectedItem.getValue().equals(PathItem.HttpMethod.PUT.name())) {
                Platform.runLater(() -> httpUtility.putRequest(Json.mapper(), mainController));
            } else if (selectedItem.getValue().equals(PathItem.HttpMethod.TRACE.name())) {
                Platform.runLater(() -> httpUtility.traceRequest(Json.mapper(), mainController));
            } else {
                mainController.showAlert("", "", selectedItem.getValue() + " not implemented yet");
                log.error(selectedItem.getValue() + " not implemented yet");
            }
        } else {
            mainController.showAlert("Please choose leaf", "", "Please choose a leaf GET,POST,....");
        }
        mainController.setIsOffloading();
    }

    public void setMainController(MainController parent, String uri, TreeItemOperatinLeaf leaf) {
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
        tableHeaders.getVisibleLeafColumn(1).setCellFactory(TextFieldTableCell.<RequestHeader>forTableColumn());
        ((TableColumn<RequestHeader, String>) tableHeaders.getVisibleLeafColumn(1)).setOnEditCommit(evt -> {
            evt.getRowValue().setName(evt.getNewValue());
            addTableRowIfFulfilled();
        });
        tableHeaders.getVisibleLeafColumn(2).setCellFactory(TextFieldTableCell.<RequestHeader>forTableColumn());
        ((TableColumn<RequestHeader, String>) tableHeaders.getVisibleLeafColumn(2)).setOnEditCommit(evt -> {
            evt.getRowValue().setValue(evt.getNewValue());
            addTableRowIfFulfilled();
        });
        onTreeItemSelect(uri, leaf);
    }
}
