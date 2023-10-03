package com.mascix.swaggerific.ui.edit;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class About implements Initializable {
    @FXML
    private WebView view;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        view.getEngine().load("https://github.com/ozkanpakdil/swaggerific");
    }
}
