package io.github.ozkanpakdil.swaggerific.ui.edit;

import io.github.ozkanpakdil.swaggerific.ui.exception.LoadFxmlRuntimeException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * make the look and feel changeable by user, - font size and family - light dark theme - .....
 */
public class SettingsController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

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
    Pane rightPane;

    public void changeSelectedSetting(ActionEvent actionEvent) {
        Button source = (Button) actionEvent.getSource();
        loadSettingsPane("/io/github/ozkanpakdil/swaggerific/edit/settings/%s.fxml".formatted(source.getText()));
    }

    private void loadSettingsPane(String name) {
        rightPane.getChildren().clear();
        try {
            rightPane.getChildren().add(new FXMLLoader(getClass().getResource(name)).load());
        } catch (IOException e) {
            throw new LoadFxmlRuntimeException(e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadSettingsPane("/io/github/ozkanpakdil/swaggerific/edit/settings/General.fxml");
    }
}
