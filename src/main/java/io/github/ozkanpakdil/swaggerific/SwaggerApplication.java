package io.github.ozkanpakdil.swaggerific;

import atlantafx.base.theme.PrimerLight;
import io.github.ozkanpakdil.swaggerific.animation.Preloader;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.prefs.Preferences;

import static io.github.ozkanpakdil.swaggerific.ui.edit.General.FONT_SIZE;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.SELECTED_FONT;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.STAGE_HEIGHT;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.STAGE_WIDTH;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.STAGE_X;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.STAGE_Y;

public class SwaggerApplication extends Application {
    private Stage primaryStage;
    Preferences userPrefs = Preferences.userNodeForPackage(getClass());
    private static final Logger log = LoggerFactory.getLogger(SwaggerApplication.class);

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        String fontSize = userPrefs.get(FONT_SIZE, ".93em");
        String selectedFont = userPrefs.get(SELECTED_FONT, "Verdana");

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        loadingWindowLookAndLocation();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-view.fxml"));
        Parent root = fxmlLoader.load();
        log.info("font size:" + fontSize);
        log.info("font family:" + selectedFont);
        MainController mainController = fxmlLoader.getController();
        mainController.onOpening();
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Swaggerific");
        stage.getIcons().add(new Image(Objects.requireNonNull(SwaggerApplication.class.getResourceAsStream("/applogo.png"))));
        stage.setScene(scene);
        stage.setOnHidden(e -> mainController.onClose());
        stage.show();
        //TODO this size change is not working, investigate. // below font change is not working on application start :(
        root.setStyle("-fx-font-size:" + fontSize + ";");
        root.setStyle("-fx-font-family:'" + selectedFont + "';");
        mainController.getTopPane().getScene().getRoot().setStyle("-fx-font-size:" + fontSize + ";");
        mainController.getTopPane().getScene().getRoot().setStyle("-fx-font-family:'" + selectedFont + "';");
    }

    private void loadingWindowLookAndLocation() {
        primaryStage.setX(userPrefs.getDouble(STAGE_X, 0));
        primaryStage.setY(userPrefs.getDouble(STAGE_Y, 0));
        primaryStage.setWidth(userPrefs.getDouble(STAGE_WIDTH, 800));
        primaryStage.setHeight(userPrefs.getDouble(STAGE_HEIGHT, 600));
    }

    @Override
    public void stop() {
        userPrefs.putDouble(STAGE_X, primaryStage.getX());
        userPrefs.putDouble(STAGE_Y, primaryStage.getY());
        userPrefs.putDouble(STAGE_WIDTH, primaryStage.getWidth());
        userPrefs.putDouble(STAGE_HEIGHT, primaryStage.getHeight());
    }

    public static void main(String[] args) {
        System.setProperty("javafx.preloader", Preloader.class.getName());
        if (log.isDebugEnabled())
            System.setProperty("jdk.httpclient.HttpClient.log", "all");
        launch();
    }
}
