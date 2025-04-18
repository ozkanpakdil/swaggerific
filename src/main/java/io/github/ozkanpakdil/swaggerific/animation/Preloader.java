package io.github.ozkanpakdil.swaggerific.animation;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Objects;

public class Preloader extends javafx.application.Preloader {
    Label label;
    ProgressBar bar;
    Stage basestage;

    @Override
    public void init() {
    }

    private Scene createPreloaderScene() {
        label = new Label();
        label.setText("Loading Application");
        bar = new ProgressBar();
        bar.setProgress(0.50);
        bar.setScaleY(2.25);
        BorderPane borderPane = new BorderPane();
        BorderPane.setAlignment(label, Pos.CENTER);
        BorderPane.setAlignment(bar, Pos.CENTER);
        borderPane.setCenter(bar);
        borderPane.setTop(label);
        borderPane.getStyleClass().add("base-pane");
        return new Scene(borderPane, 800, 600);
    }

    @Override
    public void start(Stage stage) {
        this.basestage = stage;
        basestage.initStyle(StageStyle.UNDECORATED);
        Scene sc = createPreloaderScene();
        sc.getStylesheets().addAll(Objects.requireNonNull(getClass().getResource("/css/splashscreen.css")).toString());
        stage.setScene(sc);
        stage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification scn) {
        if (scn.getType() == StateChangeNotification.Type.BEFORE_START) {
            if (basestage.isShowing()) {
                FadeTransition ft = new FadeTransition(Duration.millis(2000), basestage.getScene().getRoot());
                ft.setFromValue(1.0);
                ft.setToValue(0.0);
                final Stage s = basestage;
                EventHandler<ActionEvent> eh = t -> s.hide();
                ft.setOnFinished(eh);
                ft.play();
            }
        }
    }

    @Override
    public void handleProgressNotification(ProgressNotification pn) {
        bar.setProgress(pn.getProgress());
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification pn) {
        if (pn instanceof AppNotification) {
            bar.setProgress(((AppNotification) pn).getProgress());
            label.setText(((AppNotification) pn).getMessage());
        }
    }
}