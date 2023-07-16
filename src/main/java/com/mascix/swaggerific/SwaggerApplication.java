package com.mascix.swaggerific;

import atlantafx.base.theme.PrimerLight;
import com.mascix.swaggerific.animation.Preloader;
import com.mascix.swaggerific.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.prefs.Preferences;

import static com.mascix.swaggerific.ui.edit.SettingsController.*;

@Slf4j
public class SwaggerApplication extends Application {
    private Stage primaryStage;
    Preferences userPrefs = Preferences.userNodeForPackage(getClass());

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        loadingWindowLookAndLocation();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-view.fxml"));
        Parent root = fxmlLoader.load();
        log.info("font size:"+userPrefs.get(FONT_SIZE, ".93em"));
        log.info("font family:"+userPrefs.get(SELECTED_FONT, "Verdana"));
        //TODO this size change is not working, investigate.
        root.setStyle("-fx-font-size:" + userPrefs.get(FONT_SIZE, ".93em"));
        root.setStyle("-fx-font-family:'" + userPrefs.get(SELECTED_FONT, "Verdana") + "'");
        MainController mainController = fxmlLoader.getController();
        mainController.onOpening();
//        CustomCodeArea codeArea = new CustomCodeArea();
//        BracketHighlighter bracketHighlighter = new BracketHighlighter(codeArea);
//        mainController.setCodeJsonResponse(codeArea);
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Swaggerific");
        stage.getIcons().add(new Image(SwaggerApplication.class.getResourceAsStream("/applogo.png")));
        stage.setScene(scene);
        stage.setOnHidden(e -> mainController.onClose());
        stage.show();
    }

    private void loadingWindowLookAndLocation() {
        double x = userPrefs.getDouble(STAGE_X, 0);
        double y = userPrefs.getDouble(STAGE_Y, 0);
        double w = userPrefs.getDouble(STAGE_WIDTH, 800);
        double h = userPrefs.getDouble(STAGE_HEIGHT, 600);
        primaryStage.setX(x);
        primaryStage.setY(y);
        primaryStage.setWidth(w);
        primaryStage.setHeight(h);
    }

    @Override
    public void stop() {
        Preferences userPrefs = Preferences.userNodeForPackage(getClass());
        userPrefs.putDouble(STAGE_X, primaryStage.getX());
        userPrefs.putDouble(STAGE_Y, primaryStage.getY());
        userPrefs.putDouble(STAGE_WIDTH, primaryStage.getWidth());
        userPrefs.putDouble(STAGE_HEIGHT, primaryStage.getHeight());
    }

    public static void main(String[] args) {
        System.setProperty("javafx.preloader", Preloader.class.getName());
        launch();
    }
}
