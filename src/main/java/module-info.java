module com.mascix.swaggerific {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.fasterxml.jackson.databind;
    requires io.swagger.v3.oas.models;
    requires io.swagger.v3.core;
    requires static lombok;
    requires java.prefs;
    requires org.aspectj.runtime;
    requires org.fxmisc.richtext;
    requires org.slf4j;
    requires atlantafx.base;
    requires java.net.http;

    opens com.mascix.swaggerific to javafx.fxml;
    exports com.mascix.swaggerific;
    exports com.mascix.swaggerific.splashscreen;
    opens com.mascix.swaggerific.splashscreen to javafx.fxml;
}