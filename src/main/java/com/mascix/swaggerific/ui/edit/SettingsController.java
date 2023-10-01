package com.mascix.swaggerific.ui.edit;

import com.mascix.swaggerific.ui.MainController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * make the look and feel changeable by user,
 * - font size and family
 * - light dark theme
 * - .....
 */
@Slf4j
public class SettingsController {

    @FXML
    Button btnGeneral;
    @FXML
    Button btnThemes;
    @FXML
    Button btnShortcuts;
    @FXML
    Button btnData;
    @FXML
    Button btnAddons;
    @FXML
    Button btnCertificates;
    @FXML
    Button btnProxy;
    @FXML
    Button btnUpdate;
    @FXML
    Button btnAbout;
    @FXML
    Pane leftPane;

    public void setMainWindow(MainController mainController) {
        this.mainController = mainController;
    }

    private MainController mainController;


    public void clickGeneral(ActionEvent actionEvent)  {
        loadSettingsPane("/com/mascix/swaggerific/edit/settings/General.fxml");
    }

    private void loadSettingsPane(String name)  {
        leftPane.getChildren().clear();
        try {
            leftPane.getChildren().add(new FXMLLoader(getClass().getResource(name)).load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clickThemes(ActionEvent actionEvent) {
        loadSettingsPane("/com/mascix/swaggerific/edit/settings/Themes.fxml");
    }
}
