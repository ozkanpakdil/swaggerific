package io.github.ozkanpakdil.swaggerific.ui.edit;

import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import io.github.ozkanpakdil.swaggerific.ui.exception.NotYetImplementedException;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class General implements Initializable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(General.class);
    public static final String STAGE_X = "stage.x";
    public static final String STAGE_Y = "stage.y";
    public static final String STAGE_WIDTH = "stage.width";
    public static final String STAGE_HEIGHT = "stage.height";
    public static final String SELECTED_FONT = "selected.font";
    public static final String FONT_SIZE = "font.size";
    @FXML
    ComboBox cmbFonts;
    @FXML
    HBox fontPreview;
    @FXML
    TextField txtFontSize;

    Preferences userPrefs = Preferences.userNodeForPackage(SwaggerApplication.class);
    private MainController mainController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> families = Font.getFamilies();
        cmbFonts.setItems(FXCollections.observableArrayList(families));
        txtFontSize.setText(userPrefs.get(FONT_SIZE, "0.93em"));
    }

    public void onChange(ActionEvent actionEvent) {
        fontPreview.getChildren().clear();
        String family = (String) cmbFonts.getValue();
        log.debug("family:{}", family);
        double size = 25;

        TextFlow textFlow = new TextFlow();
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);
        Text text1 = new Text("Hello ");
        text1.setFont(Font.font(family, size));
        text1.setFill(Color.RED);
        Text text2 = new Text("Bold");
        text2.setFill(Color.ORANGE);
        text2.setFont(Font.font(family, FontWeight.BOLD, size));
        Text text3 = new Text(" World");
        text3.setFill(Color.GREEN);
        text3.setFont(Font.font(family, FontPosture.ITALIC, size));
        textFlow.getChildren().addAll(text1, text2, text3);

        fontPreview.getChildren().add(textFlow);
    }

    public void onClose() {
        String fontsValue = (String) cmbFonts.getValue();
        if (fontsValue != null && !fontsValue.isBlank()) {
            userPrefs.put(SELECTED_FONT, fontsValue);
            mainController.getTopPane().getScene().getRoot().setStyle("-fx-font-family:'" + fontsValue + "';");
            log.info("saving font family:" + fontsValue);
        }
        if (!txtFontSize.getText().isEmpty()) {
            log.debug("saving font size:" + txtFontSize.getText());
            userPrefs.put(FONT_SIZE, txtFontSize.getText());
            mainController.getTopPane().getScene().getRoot().setStyle("-fx-font-size:" + txtFontSize.getText());
        }
    }

    public void btnRestoreClick(ActionEvent actionEvent) {
        txtFontSize.setText("");
        cmbFonts.getSelectionModel().select(-1);
        userPrefs.remove(FONT_SIZE);
        userPrefs.remove(SELECTED_FONT);
    }

    public void onChangeTrimConfig(ActionEvent actionEvent) {
        mainController.onChangeTrimConfig(actionEvent);
    }

    public void onChangeSSLConfig(ActionEvent actionEvent) {
        throw new NotYetImplementedException();
    }

    public void onChangeSendNoCacheHeader(MouseEvent mouseEvent) {
        throw new NotYetImplementedException();
    }

    public void onChangeSendSwaggerTokenHeader(MouseEvent mouseEvent) {
        throw new NotYetImplementedException();
    }

    public void onChangeAutomaticallyFallowRedirects(MouseEvent mouseEvent) {
        throw new NotYetImplementedException();
    }

    public void onChangeAlwayOpenSideBarItemInNewTab(MouseEvent mouseEvent) {
        throw new NotYetImplementedException();
    }

    public void onChangeAlwaysAskWhenClosingUnsavedTabs(MouseEvent mouseEvent) {
        throw new NotYetImplementedException();
    }

    public void onChangeSendAnonymousUsageData(MouseEvent mouseEvent) {
        throw new NotYetImplementedException();
    }
}
