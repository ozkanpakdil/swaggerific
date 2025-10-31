package io.github.ozkanpakdil.swaggerific.ui.edit;

import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import io.github.ozkanpakdil.swaggerific.model.ShortcutModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Settings controller for keyboard shortcuts. Provides capture, save, and reset.
 */
public class Shortcuts implements Initializable {

    private static final String PREF_PREFIX = "shortcut.";
    // Action method names to bind with existing UI handlers
    private static final String ACTION_SEND = "btnSendRequest"; // TabRequestController#btnSendRequest
    private static final String ACTION_TOGGLE_DEBUG = "flipDebugConsole"; // MainController#flipDebugConsole

    // Additional actions for JSON folding (global scene accelerators)
    public static final String ACTION_JSON_TOGGLE = "json.toggleFoldAtCaret";
    public static final String ACTION_JSON_FOLD_TOP = "json.foldTop";
    public static final String ACTION_JSON_UNFOLD_ALL = "json.unfoldAll";

    // Default shortcuts
    private static final KeyCodeCombination DEF_SEND = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);
    private static final KeyCodeCombination DEF_DEBUG = new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN);
    private static final KeyCodeCombination DEF_JSON_TOGGLE = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN);
    private static final KeyCodeCombination DEF_JSON_FOLD_TOP = new KeyCodeCombination(KeyCode.DIGIT9, KeyCombination.CONTROL_DOWN);
    private static final KeyCodeCombination DEF_JSON_UNFOLD_ALL = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN);

    @FXML
    private TextField txtSendRequest;
    @FXML
    private TextField txtToggleDebug;
    @FXML
    private TextField txtJsonToggleFold;
    @FXML
    private TextField txtJsonFoldTop;
    @FXML
    private TextField txtJsonUnfoldAll;

    private final Preferences prefs = Preferences.userNodeForPackage(io.github.ozkanpakdil.swaggerific.SwaggerApplication.class);

    // Temporary capture state
    private enum CaptureTarget { NONE, SEND, DEBUG, JSON_TOGGLE, JSON_FOLD_TOP, JSON_UNFOLD_ALL }
    private CaptureTarget capturing = CaptureTarget.NONE;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load displayed values from preferences (fall back to defaults)
        String sendStr = prefs.get(PREF_PREFIX + ACTION_SEND, ShortcutModel.formatKeyCombination(DEF_SEND));
        String debugStr = prefs.get(PREF_PREFIX + ACTION_TOGGLE_DEBUG, ShortcutModel.formatKeyCombination(DEF_DEBUG));
        String jsonToggleStr = prefs.get(PREF_PREFIX + ACTION_JSON_TOGGLE, ShortcutModel.formatKeyCombination(DEF_JSON_TOGGLE));
        String jsonFoldTopStr = prefs.get(PREF_PREFIX + ACTION_JSON_FOLD_TOP, ShortcutModel.formatKeyCombination(DEF_JSON_FOLD_TOP));
        String jsonUnfoldAllStr = prefs.get(PREF_PREFIX + ACTION_JSON_UNFOLD_ALL, ShortcutModel.formatKeyCombination(DEF_JSON_UNFOLD_ALL));

        txtSendRequest.setText(sendStr);
        txtToggleDebug.setText(debugStr);
        if (txtJsonToggleFold != null) txtJsonToggleFold.setText(jsonToggleStr);
        if (txtJsonFoldTop != null) txtJsonFoldTop.setText(jsonFoldTopStr);
        if (txtJsonUnfoldAll != null) txtJsonUnfoldAll.setText(jsonUnfoldAllStr);

        // Add a key event filter on scene once available to capture combinations
        txtSendRequest.sceneProperty().addListener((obs, oldScene, newScene) -> attachKeyCaptureFilter(newScene));
        txtToggleDebug.sceneProperty().addListener((obs, oldScene, newScene) -> attachKeyCaptureFilter(newScene));
        if (txtJsonToggleFold != null)
            txtJsonToggleFold.sceneProperty().addListener((obs, o, n) -> attachKeyCaptureFilter(n));
        if (txtJsonFoldTop != null)
            txtJsonFoldTop.sceneProperty().addListener((obs, o, n) -> attachKeyCaptureFilter(n));
        if (txtJsonUnfoldAll != null)
            txtJsonUnfoldAll.sceneProperty().addListener((obs, o, n) -> attachKeyCaptureFilter(n));
    }

    private void attachKeyCaptureFilter(Scene scene) {
        if (scene == null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCaptureEvent);
    }

    private void handleCaptureEvent(KeyEvent e) {
        if (capturing == CaptureTarget.NONE) return;
        // Ignore pure modifier presses
        if (e.getCode() == KeyCode.SHIFT || e.getCode() == KeyCode.CONTROL || e.getCode() == KeyCode.ALT || e.getCode() == KeyCode.META) {
            e.consume();
            return;
        }
        KeyCodeCombination combo = buildCombinationFromEvent(e);
        String formatted = ShortcutModel.formatKeyCombination(combo);
        switch (capturing) {
            case SEND -> txtSendRequest.setText(formatted);
            case DEBUG -> txtToggleDebug.setText(formatted);
            case JSON_TOGGLE -> txtJsonToggleFold.setText(formatted);
            case JSON_FOLD_TOP -> txtJsonFoldTop.setText(formatted);
            case JSON_UNFOLD_ALL -> txtJsonUnfoldAll.setText(formatted);
            default -> {}
        }
        capturing = CaptureTarget.NONE;
        e.consume();
    }

    private static KeyCodeCombination buildCombinationFromEvent(KeyEvent e) {
        return new KeyCodeCombination(
                e.getCode(),
                e.isShiftDown() ? KeyCombination.SHIFT_DOWN : KeyCombination.SHIFT_ANY,
                e.isControlDown() ? KeyCombination.CONTROL_DOWN : KeyCombination.CONTROL_ANY,
                e.isAltDown() ? KeyCombination.ALT_DOWN : KeyCombination.ALT_ANY,
                e.isMetaDown() ? KeyCombination.META_DOWN : KeyCombination.META_ANY,
                KeyCombination.SHORTCUT_ANY
        );
    }

    @FXML
    public void beginCaptureSend() {
        capturing = CaptureTarget.SEND;
        txtSendRequest.setPromptText("Press desired keys…");
    }

    @FXML
    public void beginCaptureDebug() {
        capturing = CaptureTarget.DEBUG;
        txtToggleDebug.setPromptText("Press desired keys…");
    }

    @FXML
    public void beginCaptureJsonToggle() {
        capturing = CaptureTarget.JSON_TOGGLE;
        txtJsonToggleFold.setPromptText("Press desired keys…");
    }

    @FXML
    public void beginCaptureJsonFoldTop() {
        capturing = CaptureTarget.JSON_FOLD_TOP;
        txtJsonFoldTop.setPromptText("Press desired keys…");
    }

    @FXML
    public void beginCaptureJsonUnfoldAll() {
        capturing = CaptureTarget.JSON_UNFOLD_ALL;
        txtJsonUnfoldAll.setPromptText("Press desired keys…");
    }

    @FXML
    public void clearSend() {
        txtSendRequest.clear();
    }

    @FXML
    public void clearDebug() {
        txtToggleDebug.clear();
    }

    @FXML
    public void clearJsonToggle() { if (txtJsonToggleFold != null) txtJsonToggleFold.clear(); }
    @FXML
    public void clearJsonFoldTop() { if (txtJsonFoldTop != null) txtJsonFoldTop.clear(); }
    @FXML
    public void clearJsonUnfoldAll() { if (txtJsonUnfoldAll != null) txtJsonUnfoldAll.clear(); }

    @FXML
    public void save() {
        // Validate and persist; empty text removes custom preference
        persistShortcut(ACTION_SEND, txtSendRequest.getText(), DEF_SEND);
        persistShortcut(ACTION_TOGGLE_DEBUG, txtToggleDebug.getText(), DEF_DEBUG);
        // JSON folding
        persistShortcut(ACTION_JSON_TOGGLE, txtJsonToggleFold != null ? txtJsonToggleFold.getText() : null, DEF_JSON_TOGGLE);
        persistShortcut(ACTION_JSON_FOLD_TOP, txtJsonFoldTop != null ? txtJsonFoldTop.getText() : null, DEF_JSON_FOLD_TOP);
        persistShortcut(ACTION_JSON_UNFOLD_ALL, txtJsonUnfoldAll != null ? txtJsonUnfoldAll.getText() : null, DEF_JSON_UNFOLD_ALL);
        // Live-apply across windows
        try {
            SwaggerApplication app = SwaggerApplication.getInstance();
            if (app != null) {
                app.applyCustomShortcutsToAllWindows();
            }
        } catch (Exception ignored) { }
    }

    @FXML
    public void resetDefaults() {
        txtSendRequest.setText(ShortcutModel.formatKeyCombination(DEF_SEND));
        txtToggleDebug.setText(ShortcutModel.formatKeyCombination(DEF_DEBUG));
        if (txtJsonToggleFold != null) txtJsonToggleFold.setText(ShortcutModel.formatKeyCombination(DEF_JSON_TOGGLE));
        if (txtJsonFoldTop != null) txtJsonFoldTop.setText(ShortcutModel.formatKeyCombination(DEF_JSON_FOLD_TOP));
        if (txtJsonUnfoldAll != null) txtJsonUnfoldAll.setText(ShortcutModel.formatKeyCombination(DEF_JSON_UNFOLD_ALL));
        save();
    }

    private void persistShortcut(String action, String value, KeyCodeCombination def) {
        String key = PREF_PREFIX + action;
        if (value == null || value.isBlank()) {
            // Remove to fall back to default behavior
            prefs.remove(key);
            return;
        }
        KeyCodeCombination parsed = ShortcutModel.parseShortcut(value);
        if (parsed == null) {
            // If cannot parse, store default
            prefs.put(key, ShortcutModel.formatKeyCombination(def));
        } else {
            prefs.put(key, ShortcutModel.formatKeyCombination(parsed));
        }
    }
}
