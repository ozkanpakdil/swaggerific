package io.github.ozkanpakdil.swaggerific.ui.edit;

import atlantafx.base.controls.ToggleSwitch;
import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpServiceImpl;
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

    // General settings keys
    public static final String KEY_TRIM_BODY = "http.trimRequestBody";
    public static final String KEY_SSL_VERIFY = "http.ssl.verify"; // true means verify enabled
    public static final String KEY_TIMEOUT_MS = "http.requestTimeoutMs";
    public static final String KEY_MAX_RESPONSE_SIZE = "http.maxResponseSizeBytes";
    public static final String KEY_NO_CACHE = "http.header.noCache";
    public static final String KEY_SWAGGER_TOKEN = "http.header.swaggerToken";
    public static final String KEY_FOLLOW_REDIRECTS = "http.followRedirects";
    public static final String KEY_OPEN_SIDEBAR_IN_NEW_TAB = "ui.sidebar.openInNewTab";
    public static final String KEY_ASK_WHEN_CLOSING_UNSAVED = "ui.askWhenClosingUnsaved";
    public static final String KEY_SEND_ANONYMOUS_USAGE = "analytics.sendAnonymousUsage";

    @FXML
    ComboBox cmbFonts;
    @FXML
    HBox fontPreview;
    @FXML
    TextField txtFontSize;

    // Controls from FXML
    @FXML
    private ToggleSwitch chkTrim;
    @FXML
    private ToggleSwitch chkSSLVerification;
    @FXML
    private TextField txtRequestTimeout;
    @FXML
    private TextField txtMaxResponseSize;
    @FXML
    private ToggleSwitch chkSendNoCacheHeader;
    @FXML
    private ToggleSwitch chkSendSwaggerTokenHeader;
    @FXML
    private ToggleSwitch chkAutomaticallyFallowRedirects;
    @FXML
    private ToggleSwitch chkAlwayOpenSideBarItemInNewTab;
    @FXML
    private ToggleSwitch chkAlwaysAskWhenClosingUnsavedTabs;
    @FXML
    private ToggleSwitch chkSendAnonymousUsageData;

    Preferences userPrefs = Preferences.userNodeForPackage(SwaggerApplication.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> families = Font.getFamilies();
        cmbFonts.setItems(FXCollections.observableArrayList(families));
        txtFontSize.setText(userPrefs.get(FONT_SIZE, "0.93em"));

        // Load settings into controls
        chkTrim.setSelected(userPrefs.getBoolean(KEY_TRIM_BODY, false));
        boolean sslVerify = userPrefs.getBoolean(KEY_SSL_VERIFY, true);
        chkSSLVerification.setSelected(sslVerify);
        txtRequestTimeout.setText(String.valueOf(userPrefs.getInt(KEY_TIMEOUT_MS, 30000)));
        txtMaxResponseSize.setText(String.valueOf(userPrefs.getInt(KEY_MAX_RESPONSE_SIZE, 2_000_000)));
        chkSendNoCacheHeader.setSelected(userPrefs.getBoolean(KEY_NO_CACHE, false));
        chkSendSwaggerTokenHeader.setSelected(userPrefs.getBoolean(KEY_SWAGGER_TOKEN, false));
        chkAutomaticallyFallowRedirects.setSelected(userPrefs.getBoolean(KEY_FOLLOW_REDIRECTS, true));
        chkAlwayOpenSideBarItemInNewTab.setSelected(userPrefs.getBoolean(KEY_OPEN_SIDEBAR_IN_NEW_TAB, false));
        chkAlwaysAskWhenClosingUnsavedTabs.setSelected(userPrefs.getBoolean(KEY_ASK_WHEN_CLOSING_UNSAVED, true));
        // Persist after the toggle state actually changes (mouse event may fire before selected flips)
        chkAlwaysAskWhenClosingUnsavedTabs.selectedProperty().addListener((obs, oldVal, newVal) -> {
            userPrefs.putBoolean(KEY_ASK_WHEN_CLOSING_UNSAVED, newVal);
            log.info("Always ask when closing unsaved tabs set to {}", newVal);
        });
        chkSendAnonymousUsageData.setSelected(userPrefs.getBoolean(KEY_SEND_ANONYMOUS_USAGE, false));
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
            log.info("saving font family:" + fontsValue);
        }
        if (!txtFontSize.getText().isEmpty()) {
            log.debug("saving font size:" + txtFontSize.getText());
            userPrefs.put(FONT_SIZE, txtFontSize.getText());
        }
        // Save numeric fields
        saveTimeout();
        saveMaxResponseSize();
    }

    public void btnRestoreClick(ActionEvent actionEvent) {
        txtFontSize.setText("");
        cmbFonts.getSelectionModel().select(-1);
        userPrefs.remove(FONT_SIZE);
        userPrefs.remove(SELECTED_FONT);

        // Reset toggles and numeric fields to defaults
        chkTrim.setSelected(false);
        chkSSLVerification.setSelected(true);
        chkSendNoCacheHeader.setSelected(false);
        chkSendSwaggerTokenHeader.setSelected(false);
        chkAutomaticallyFallowRedirects.setSelected(true);
        chkAlwayOpenSideBarItemInNewTab.setSelected(false);
        chkAlwaysAskWhenClosingUnsavedTabs.setSelected(true);
        chkSendAnonymousUsageData.setSelected(false);
        txtRequestTimeout.setText("30000");
        txtMaxResponseSize.setText("2000000");

        // Clear prefs
        userPrefs.putBoolean(KEY_TRIM_BODY, false);
        userPrefs.putBoolean(KEY_SSL_VERIFY, true);
        userPrefs.putBoolean(KEY_NO_CACHE, false);
        userPrefs.putBoolean(KEY_SWAGGER_TOKEN, false);
        userPrefs.putBoolean(KEY_FOLLOW_REDIRECTS, true);
        userPrefs.putBoolean(KEY_OPEN_SIDEBAR_IN_NEW_TAB, false);
        userPrefs.putBoolean(KEY_ASK_WHEN_CLOSING_UNSAVED, true);
        userPrefs.putBoolean(KEY_SEND_ANONYMOUS_USAGE, false);
        userPrefs.putInt(KEY_TIMEOUT_MS, 30000);
        userPrefs.putInt(KEY_MAX_RESPONSE_SIZE, 2_000_000);

        // Recreate clients to apply network changes
        HttpServiceImpl.recreateAllHttpClients();
    }

    public void onChangeTrimConfig(ActionEvent actionEvent) {
        boolean selected = chkTrim.isSelected();
        userPrefs.putBoolean(KEY_TRIM_BODY, selected);
        log.info("Trim request body set to {}", selected);
    }

    public void onChangeSSLConfig(ActionEvent actionEvent) {
        boolean verifyEnabled = chkSSLVerification.isSelected();
        userPrefs.putBoolean(KEY_SSL_VERIFY, verifyEnabled);
        // ProxySettings expects 'disableSslValidation' flag, so mirror value there
        Preferences.userNodeForPackage(SwaggerApplication.class)
                .putBoolean("disableSslValidation", !verifyEnabled);
        log.info("SSL certificate verification {}", verifyEnabled ? "ENABLED" : "DISABLED");
        // Recreate clients to apply SSL changes
        HttpServiceImpl.recreateAllHttpClients();
    }

    public void onChangeSendNoCacheHeader(MouseEvent mouseEvent) {
        boolean selected = chkSendNoCacheHeader.isSelected();
        userPrefs.putBoolean(KEY_NO_CACHE, selected);
        log.info("Send no-cache header set to {}", selected);
    }

    public void onChangeSendSwaggerTokenHeader(MouseEvent mouseEvent) {
        boolean selected = chkSendSwaggerTokenHeader.isSelected();
        userPrefs.putBoolean(KEY_SWAGGER_TOKEN, selected);
        log.info("Send swagger token header set to {}", selected);
    }

    public void onChangeAutomaticallyFallowRedirects(MouseEvent mouseEvent) {
        boolean selected = chkAutomaticallyFallowRedirects.isSelected();
        userPrefs.putBoolean(KEY_FOLLOW_REDIRECTS, selected);
        log.info("Follow redirects set to {}", selected);
        HttpServiceImpl.recreateAllHttpClients();
    }

    public void onChangeAlwayOpenSideBarItemInNewTab(MouseEvent mouseEvent) {
        boolean selected = chkAlwayOpenSideBarItemInNewTab.isSelected();
        userPrefs.putBoolean(KEY_OPEN_SIDEBAR_IN_NEW_TAB, selected);
        log.info("Always open sidebar item in new tab set to {}", selected);
    }

    public void onChangeAlwaysAskWhenClosingUnsavedTabs(MouseEvent mouseEvent) {
        boolean selected = chkAlwaysAskWhenClosingUnsavedTabs.isSelected();
        userPrefs.putBoolean(KEY_ASK_WHEN_CLOSING_UNSAVED, selected);
        log.info("Always ask when closing unsaved tabs set to {}", selected);
    }

    public void onChangeSendAnonymousUsageData(MouseEvent mouseEvent) {
        boolean selected = chkSendAnonymousUsageData.isSelected();
        userPrefs.putBoolean(KEY_SEND_ANONYMOUS_USAGE, selected);
        log.info("Send anonymous usage data set to {}", selected);
    }

    @FXML
    public void onTimeoutChanged(ActionEvent ignored) {
        saveTimeout();
    }

    @FXML
    public void onMaxResponseSizeChanged(ActionEvent ignored) {
        saveMaxResponseSize();
    }

    private void saveTimeout() {
        try {
            int ms = Integer.parseInt(txtRequestTimeout.getText().trim());
            if (ms < 0) throw new NumberFormatException();
            userPrefs.putInt(KEY_TIMEOUT_MS, ms);
            HttpServiceImpl.recreateAllHttpClients();
        } catch (NumberFormatException e) {
            log.warn("Invalid timeout value: {}", txtRequestTimeout.getText());
        }
    }

    private void saveMaxResponseSize() {
        try {
            int bytes = Integer.parseInt(txtMaxResponseSize.getText().trim());
            if (bytes < 1024) throw new NumberFormatException();
            userPrefs.putInt(KEY_MAX_RESPONSE_SIZE, bytes);
        } catch (NumberFormatException e) {
            log.warn("Invalid max response size: {}", txtMaxResponseSize.getText());
        }
    }
}
