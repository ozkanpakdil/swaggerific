module com.mascix.swaggerific {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.fasterxml.jackson.databind;
    requires io.swagger.v3.oas.models;
    requires static lombok;

    opens com.mascix.swaggerific to javafx.fxml;
    exports com.mascix.swaggerific;
}