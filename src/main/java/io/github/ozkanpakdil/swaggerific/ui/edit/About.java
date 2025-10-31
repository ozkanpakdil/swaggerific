package io.github.ozkanpakdil.swaggerific.ui.edit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

public class About implements Initializable {

    @FXML private Label lblAppName;
    @FXML private Label lblVersion;
    @FXML private Hyperlink lnkHomepage;
    @FXML private Hyperlink lnkIssues;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblAppName.setText("Swaggerific");
        String version = "unknown";
        try (var stream = getClass().getResourceAsStream("/application.properties")) {
            if (stream != null) {
                Properties p = new Properties();
                p.load(stream);
                version = p.getProperty("app.version", p.getProperty("project.version", "unknown"));
            }
        } catch (IOException ignored) { }
        lblVersion.setText("Version: " + version);
        lnkHomepage.setText("Project Homepage");
        lnkIssues.setText("Report an Issue");
    }

    @FXML
    public void openHomepage(ActionEvent e) {
        browse("https://github.com/ozkanpakdil/swaggerific");
    }

    @FXML
    public void openIssues(ActionEvent e) {
        browse("https://github.com/ozkanpakdil/swaggerific/issues");
    }

    private void browse(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (IOException ignored) { }
    }
}
