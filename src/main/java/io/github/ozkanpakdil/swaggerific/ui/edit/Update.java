package io.github.ozkanpakdil.swaggerific.ui.edit;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

public class Update {
    private static final Logger log = LoggerFactory.getLogger(Update.class);
    @FXML
    Label versionLabel;
    @FXML
    Label updateStatusLabel;
    @FXML
    Button downloadUpdateButton;
    String version;

    public void initialize() throws Exception {
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        version = props.getProperty("project.version");
        versionLabel.setText(
                "Swaggerific v" + version + " is the current version. "
        );
    }

    public void checkUpdate(ActionEvent actionEvent) {
        String githubReleasesUrl = "https://api.github.com/repos/ozkanpakdil/swaggerific/releases/latest";

        try {
            URL url = new URL(githubReleasesUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonNode jsonNode = Json.mapper().readTree(response.toString());
                String latestVersion = jsonNode.get("tag_name").asText();

                if (!latestVersion.equals(version)) {
                    log.info("Update available: " + latestVersion);
                    updateStatusLabel.setText("Update available: " + latestVersion);
                } else {
                    updateStatusLabel.setText("No update available");
                    log.info("No update available");
                }
                downloadUpdateButton.setVisible(true);
                updateStatusLabel.setVisible(true);
            } else {
                log.info("Failed to retrieve latest version");
            }
        } catch (Exception e) {
            log.error("Error checking for update: {}", e.getMessage(), e);
        }
    }

    public void downloadUpdate(ActionEvent actionEvent) throws Exception {
        String latestDownloadUrl = "https://github.com/ozkanpakdil/swaggerific/releases";
        Desktop.getDesktop().browse(new URI(latestDownloadUrl));
    }
}