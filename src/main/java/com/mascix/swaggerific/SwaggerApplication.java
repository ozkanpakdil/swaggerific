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

import java.io.IOException;
import java.util.prefs.Preferences;

public class SwaggerApplication extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        loadingWindowLookAndLocation();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-view.fxml"));
        Parent root = fxmlLoader.load();
        MainController mainController = fxmlLoader.getController();
        mainController.onOpening();
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Swaggerific");
        stage.getIcons().add(new Image(SwaggerApplication.class.getResourceAsStream("/applogo.png")));
        stage.setScene(scene);
        stage.setOnHidden(e -> mainController.shutdown());
        stage.show();
    }

    private void loadingWindowLookAndLocation() {
        Preferences userPrefs = Preferences.userNodeForPackage(getClass());
        double x = userPrefs.getDouble("stage.x", 0);
        double y = userPrefs.getDouble("stage.y", 0);
        double w = userPrefs.getDouble("stage.width", 800);
        double h = userPrefs.getDouble("stage.height", 600);
        primaryStage.setX(x);
        primaryStage.setY(y);
        primaryStage.setWidth(w);
        primaryStage.setHeight(h);
    }

    @Override
    public void stop() {
        Preferences userPrefs = Preferences.userNodeForPackage(getClass());
        userPrefs.putDouble("stage.x", primaryStage.getX());
        userPrefs.putDouble("stage.y", primaryStage.getY());
        userPrefs.putDouble("stage.width", primaryStage.getWidth());
        userPrefs.putDouble("stage.height", primaryStage.getHeight());
    }

    public static void main(String[] args) {
        System.setProperty("javafx.preloader", Preloader.class.getName());
        launch();
    }
}
