package io.github.ozkanpakdil.swaggerific.ui.edit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import org.controlsfx.control.ToggleSwitch;

import java.io.File;

public class Certificates {
    @FXML
    private ToggleSwitch togglePEM;
    @FXML
    private Button choosePEMFile;

    @FXML
    public void initialize() {
        choosePEMFile.managedProperty().bind(togglePEM.selectedProperty());
    }

    public void choosePEMFileClick(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open PEM File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PEM Files", "*.pem"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            // Handle the selected file
        }
    }

    public void addCertificate(ActionEvent actionEvent) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}