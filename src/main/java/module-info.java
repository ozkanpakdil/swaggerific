module com.mascix.swaggerific {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.media;
    requires javafx.swing;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.ikonli.fontawesome5;
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
    requires jakarta.ws.rs;
    requires org.apache.commons.lang3;

//    requires swagger.parser.core;
//    requires swagger.parser;
//    requires swagger.parser.v2.converter;
//    requires swagger.parser.v3;

    opens com.mascix.swaggerific to javafx.fxml;
    exports com.mascix.swaggerific;
    exports com.mascix.swaggerific.animation;
    opens com.mascix.swaggerific.animation to javafx.fxml;
    exports com.mascix.swaggerific.ui;
    opens com.mascix.swaggerific.ui to javafx.fxml;
    exports com.mascix.swaggerific.data;
    opens com.mascix.swaggerific.data to javafx.fxml;
    exports com.mascix.swaggerific.ui.component;
    opens com.mascix.swaggerific.ui.component to javafx.fxml;
    exports com.mascix.swaggerific.ui.edit;
    opens com.mascix.swaggerific.ui.edit to javafx.fxml;
    exports com.mascix.swaggerific.ui.textfx;
    opens com.mascix.swaggerific.ui.textfx to javafx.fxml;
}