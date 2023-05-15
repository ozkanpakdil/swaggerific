package com.mascix.swaggerific;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.PathItem;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    VBox mainBox;
    @FXML
    TextArea txtJson;

    @FXML
    TreeView treePaths;

    @FXML
    AnchorPane ancText;
    SwaggerModal jsonModal;
    ObjectMapper mapper = new ObjectMapper();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /*
        showAlert();*/
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//        SwingNode swingNode = new SwingNode();
//        RSyntaxTextArea textArea = new RSyntaxTextArea(200, 160);
//        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
//        textArea.setCodeFoldingEnabled(true);
//        textArea.setLineWrap(true);
//        textArea.setAutoIndentEnabled(true);
//        textArea.setAutoscrolls(true);
//        RTextScrollPane sp = new RTextScrollPane(textArea);
//        sp.setAutoscrolls(true);
//        sp.setLineNumbersEnabled(true);
//        swingNode.setContent(sp);
//        ancText.getChildren().add(swingNode);

        txtJson.setWrapText(true);
        treePaths.getSelectionModel()
                .selectedItemProperty()
                .addListener((ChangeListener<TreeItem<String>>) (observable, oldValue, newValue) -> {
                    // newValue represents the selected itemTree
                    if (newValue instanceof MyTreeItem) {
                        MyTreeItem m = (MyTreeItem) newValue;
                        try {
                            jsonModal.getPaths().forEach((k, v) -> {
//                            if(v.)
//                            v.readOperationsMap().get(newValue.getValue()).getParameters()
                            });
                            PathItem p = m.getBindPathItem();
                            txtJson.setText(
                                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(p)
                            );
                            System.out.println("new:" + newValue.getValue());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    public void showAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Message Here...");
        alert.setHeaderText("Look, an Information Dialog");
        alert.setContentText("I have a great message for you!");
        alert.showAndWait().ifPresent(rs -> {
            if (rs == ButtonType.OK) {
                System.out.println("Pressed OK.");
            }
        });
    }

    public void handleAboutAction(ActionEvent actionEvent) {
        showAlert();
    }

    public void menuFileExit(ActionEvent actionEvent) {
        Platform.exit();
        System.exit(0);
    }

    public void menuFileOpenSwagger(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog("https://petstore.swagger.io/v2/swagger.json");
        dialog.setTitle("Enter swagger url");
        dialog.setContentText("URL:");
        dialog.setHeaderText("Enter the json url of swagger url.");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(string -> {
            mainBox.setDisable(true);
            openSwaggerUrl(string);
            mainBox.setDisable(false);
        });
    }

    @DisableWindow
    private void openSwaggerUrl(String urlSwagger) {
        String webApiUrl = urlSwagger;
        TreeItem<String> root = new TreeItem<>("base root");
        treePaths.setRoot(root);
        try {
            jsonModal = mapper.readValue(new URL(webApiUrl), SwaggerModal.class);
            jsonModal.getTags().forEach(it -> {
                TreeItem<String> tag = new TreeItem<>();
                tag.setValue(it.getName());
                jsonModal.getPaths().forEach((it2, pathItem) -> {
                    if (it2.contains(it.getName())) {
                        TreeItem path = new TreeItem();
                        path.setValue(it2);
                        tag.getChildren().add(path);
                        returnTreeItemsForTheMethod(pathItem, path.getChildren());
                    }
                });
                root.getChildren().add(tag);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        treePaths.setShowRoot(false);

    }

    private void returnTreeItemsForTheMethod(PathItem pathItem, ObservableList<TreeItem<String>> children) {
        pathItem.readOperationsMap().forEach((k, v) -> {
            MyTreeItem it = new MyTreeItem();
            it.setValue(k.name());
            it.setBindPathItem(pathItem);
            children.add(it);
        });
    }
}