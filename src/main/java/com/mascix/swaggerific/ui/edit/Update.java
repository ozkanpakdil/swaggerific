package com.mascix.swaggerific.ui.edit;

import com.mascix.swaggerific.ui.exception.NotYetImplementedException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Update {
    @FXML
    private Label versionLabel;

    public void initialize() {
        versionLabel.setText("Swaggerific v0.0.2 is the latest version.");
    }

    public void checkUpdate(ActionEvent actionEvent) {
        throw new NotYetImplementedException("This feature is not implemented yet.");
    }
}