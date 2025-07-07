module swaggerific {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.base;
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
    requires java.prefs;
    requires org.aspectj.runtime;
    requires org.fxmisc.richtext;
    requires org.slf4j;
    requires atlantafx.base;
    requires java.net.http;
    requires jakarta.ws.rs;
    requires org.apache.commons.lang3;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires org.dockfx;
    requires org.jetbrains.annotations;
    requires java.scripting;

    // GraalVM Truffle modules for JavaScript execution
    requires org.graalvm.polyglot;
    requires org.graalvm.truffle;
	//opens com.oracle.truffle.polyglot;

    // GraalVM native image support for substitutions
    //requires static org.graalvm.nativeimage;

    //    requires swagger.parser.core;
    //    requires swagger.parser;
    //    requires swagger.parser.v2.converter;
    //    requires swagger.parser.v3;

    opens io.github.ozkanpakdil.swaggerific to javafx.fxml;
    exports io.github.ozkanpakdil.swaggerific;
    exports io.github.ozkanpakdil.swaggerific.animation;
    opens io.github.ozkanpakdil.swaggerific.animation to javafx.fxml;
    exports io.github.ozkanpakdil.swaggerific.ui;
    opens io.github.ozkanpakdil.swaggerific.ui to javafx.fxml;
    exports io.github.ozkanpakdil.swaggerific.data;
    opens io.github.ozkanpakdil.swaggerific.data to javafx.fxml;
    exports io.github.ozkanpakdil.swaggerific.ui.component;
    opens io.github.ozkanpakdil.swaggerific.ui.component to javafx.fxml;
    exports io.github.ozkanpakdil.swaggerific.ui.edit;
    opens io.github.ozkanpakdil.swaggerific.ui.edit to javafx.fxml;
    exports io.github.ozkanpakdil.swaggerific.ui.textfx;
    opens io.github.ozkanpakdil.swaggerific.ui.textfx to javafx.fxml;
    exports io.github.ozkanpakdil.swaggerific.ui.exception;
    opens io.github.ozkanpakdil.swaggerific.ui.exception to javafx.fxml;
    exports io.github.ozkanpakdil.swaggerific.model;
    opens io.github.ozkanpakdil.swaggerific.model to javafx.base, javafx.fxml;
}
