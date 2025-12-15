package io.github.ozkanpakdil.swaggerific.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class MenuController {
    @FXML
    private MainController mainController;

    public void menuFileOpenSwagger(ActionEvent event) {
        mainController.menuFileOpenSwagger(event);
    }

    public void menuFileOpenLocal(ActionEvent event) {
        mainController.menuFileOpenLocal(event);
    }

    public void openDebugConsole() {
        mainController.flipDebugConsole();
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
    
    public void openEnvironmentVariables(ActionEvent event) {
        mainController.openEnvironmentVariables(event);
    }

    public void menuRequestSend(ActionEvent event) {
        if (mainController != null) {
            mainController.requestSendFromMenu();
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}