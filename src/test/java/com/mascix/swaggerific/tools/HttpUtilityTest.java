package com.mascix.swaggerific.tools;

import com.mascix.swaggerific.ui.component.STextField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ApplicationExtension.class)
class HttpUtilityTest {

    HttpUtility testee = new HttpUtility();
    private TextField txtField;
    private GridPane gridPane;

    @Start
    private void start(Stage stage) {
        txtField = new TextField();
        gridPane = new GridPane();
    }

    @Test
    void postRequest() {
    }

    @Test
    void getRequest() {
    }

    @Test
    void getUri() {
        txtField.setText("https://petstore.swagger.io/v2/pet/findByStatus");
        addParam("status", "sold", "query");
        addParam("any", "many", "query");
        URI uri = testee.getUri(txtField, gridPane);
        assertEquals(URI.create("https://petstore.swagger.io/v2/pet/findByStatus?status=sold&any=many"), uri);
    }

    @Test
    void getUriPath() {
        txtField.setText("https://petstore.swagger.io/v2/pet/{findByStatus}");
        addParam("findByStatus", "sold", "query");
        URI uri = testee.getUri(txtField, gridPane);
        assertEquals(URI.create("https://petstore.swagger.io/v2/pet/sold?findByStatus=sold"), uri);
    }

    private void addParam(String paramName, String txtText, String varType) {
        STextField sTextField = new STextField();
        sTextField.setParamName(paramName);
        sTextField.setText(txtText);
        sTextField.setIn(varType);
        gridPane.getChildren().add(sTextField);
    }
}