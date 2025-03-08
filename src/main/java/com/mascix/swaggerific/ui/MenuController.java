package com.mascix.swaggerific.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import lombok.Setter;

@Setter
public class MenuController {
    @FXML
    private MainController mainController;

    public void menuFileOpenSwagger(ActionEvent event) {
        mainController.menuFileOpenSwagger(event);
    }

    public void openDebugConsole() {
        mainController.openDebugConsole();
    }

    public void menuFileExit(ActionEvent event) {
        mainController.menuFileExit(event);
    }

    public void openSettings(ActionEvent event) {
        mainController.openSettings(event);
    }

    public void showHideTree(ActionEvent event) {
        mainController.showHideTree(event);
    }

    public void showHideFilter(ActionEvent event) {
        mainController.showHideFilter(event);
    }

    public void showHideStatusBar(ActionEvent event) {
        mainController.showHideStatusBar(event);
    }

    public void expandAllTree(ActionEvent event) {
        mainController.expandAllTree(event);
    }

    public void collapseAllTree(ActionEvent event) {
        mainController.collapseAllTree(event);
    }

    public void handleAboutAction(ActionEvent event) {
        mainController.handleAboutAction(event);
    }

    public void reportBugOrFeatureRequestFromHelpMenu(ActionEvent event) {
        mainController.reportBugOrFeatureRequestFromHelpMenu(event);
    }
}