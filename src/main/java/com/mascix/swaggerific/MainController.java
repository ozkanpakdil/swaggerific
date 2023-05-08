package com.mascix.swaggerific;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    ObjectMapper mapper = new ObjectMapper();
    @FXML
    private TreeView treePaths;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /*Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Message Here...");
        alert.setHeaderText("Look, an Information Dialog");
        alert.setContentText("I have a great message for you!");
        alert.showAndWait().ifPresent(rs -> {
            if (rs == ButtonType.OK) {
                System.out.println("Pressed OK.");
            }
        });
        showAlert();*/
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String webApiUrl = "https://petstore.swagger.io/v2/swagger.json";
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

    public void showAlert() {
        Platform.runLater(new Runnable() {
            public void run() {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Message Here...");
                alert.setHeaderText("Look, an Information Dialog");
                alert.setContentText("I have a great message for you!");
                alert.showAndWait();
            }
        });
    }
}