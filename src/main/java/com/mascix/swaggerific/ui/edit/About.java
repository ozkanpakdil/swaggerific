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
        //TODO webview is not working, throwing "at javafx.web@21/com.sun.javafx.webkit.prism.WCPageBackBufferImpl.validate(WCPageBackBufferImpl.java:100)" could not find a way to fix this.
//        view.getEngine().load("https://github.com/ozkanpakdil/swaggerific");

//        WebView webView = new WebView();
//
//        webView.getEngine().load("http://google.com");
        view.getEngine().loadContent("<html><body><h1>My Page</h1></body></html>");
    }
}
