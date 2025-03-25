package io.github.ozkanpakdil.swaggerific.animation;

import javafx.application.Preloader;

public class AppNotification
        implements Preloader.PreloaderNotification {
    private final String msg;
    private final Double prog;

    public AppNotification(String message, Double progress) {
        msg = message;
        prog = progress;
    }

    public String getMessage() {
        return msg;
    }

    public Double getProgress() {
        return prog;
    }

}