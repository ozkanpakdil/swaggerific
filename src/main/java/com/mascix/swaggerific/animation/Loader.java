package com.mascix.swaggerific.animation;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class Loader {
    private static ProgressIndicator progressIndicator;

    public static void enableLoadingAnimation(Window owner) {
        Platform.runLater(() -> {
            Stage loaderStage = new Stage();
            loaderStage.initOwner(owner);
            loaderStage.initModality(Modality.APPLICATION_MODAL);

            VBox vbox = new VBox();
            vbox.setAlignment(Pos.CENTER);
            vbox.setSpacing(10);

            progressIndicator = new ProgressIndicator();
            vbox.getChildren().addAll(progressIndicator);

            loaderStage.setScene(new Scene(vbox, 200, 200));
            loaderStage.show();
        });
    }

    public static void disableLoadingAnimation() {
        Platform.runLater(() -> {
            if (progressIndicator != null) {
                Stage loaderStage = (Stage) progressIndicator.getScene().getWindow();
                loaderStage.close();
                progressIndicator = null;
            }
        });
    }
}
