package com.mascix.swaggerific;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    VBox mainBox;

    ObjectMapper mapper = new ObjectMapper();
    @FXML
    private TreeView treePaths;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /*
        showAlert();*/
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

    public void handleKeyInput(KeyEvent keyEvent) {

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
            SwaggerModal jsonNode = mapper.readValue(new URL(webApiUrl), SwaggerModal.class);
            jsonNode.getTags().forEach(it -> {
                TreeItem<String> tag = new TreeItem<>();
                tag.setValue(it.getName());
                jsonNode.getPaths().forEach((it2, pathItem) -> {
                    if (it2.contains(it.getName())) {
                        TreeItem<String> path = new TreeItem<>();
                        path.setValue(it2);
                        tag.getChildren().add(path);
                    }
                });
                root.getChildren().add(tag);
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        treePaths.setShowRoot(false);
    }
}