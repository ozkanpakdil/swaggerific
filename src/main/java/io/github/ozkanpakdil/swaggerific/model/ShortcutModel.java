package io.github.ozkanpakdil.swaggerific.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Model class for keyboard shortcuts.
 */
public class ShortcutModel {
    private final StringProperty action = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty shortcut = new SimpleStringProperty();
    private KeyCodeCombination keyCombination;

    public ShortcutModel(String action, String description, KeyCodeCombination keyCombination) {
        this.action.set(action);
        this.description.set(description);
        this.keyCombination = keyCombination;
        this.shortcut.set(formatKeyCombination(keyCombination));
    }

    public String getAction() {
        return action.get();
    }

    public StringProperty actionProperty() {
        return action;
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public String getShortcut() {
        return shortcut.get();
    }

    public StringProperty shortcutProperty() {
        return shortcut;
    }

    public KeyCodeCombination getKeyCombination() {
        return keyCombination;
    }

    public void setKeyCombination(KeyCodeCombination keyCombination) {
        this.keyCombination = keyCombination;
        this.shortcut.set(formatKeyCombination(keyCombination));
    }

    /**
     * Formats a KeyCodeCombination into a human-readable string.
     */
    public static String formatKeyCombination(KeyCodeCombination combination) {
        if (combination == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        
        if (combination.getAlt() == KeyCombination.ModifierValue.DOWN) {
            sb.append("Alt+");
        }
        
        if (combination.getControl() == KeyCombination.ModifierValue.DOWN) {
            sb.append("Ctrl+");
        }
        
        if (combination.getShift() == KeyCombination.ModifierValue.DOWN) {
            sb.append("Shift+");
        }
        
        if (combination.getMeta() == KeyCombination.ModifierValue.DOWN) {
            sb.append("Meta+");
        }
        
        sb.append(combination.getCode().getName());
        
        return sb.toString();
    }

    /**
     * Parses a human-readable shortcut string into a KeyCodeCombination.
     */
    public static KeyCodeCombination parseShortcut(String shortcutStr) {
        if (shortcutStr == null || shortcutStr.isEmpty()) {
            return null;
        }

        boolean hasAlt = shortcutStr.contains("Alt+");
        boolean hasCtrl = shortcutStr.contains("Ctrl+");
        boolean hasShift = shortcutStr.contains("Shift+");
        boolean hasMeta = shortcutStr.contains("Meta+");

        String keyName = shortcutStr
                .replace("Alt+", "")
                .replace("Ctrl+", "")
                .replace("Shift+", "")
                .replace("Meta+", "");

        KeyCode keyCode = KeyCode.getKeyCode(keyName);
        if (keyCode == null) {
            return null;
        }

        return new KeyCodeCombination(
                keyCode,
                hasShift ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP,
                hasCtrl ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP,
                hasAlt ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP,
                hasMeta ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP,
                KeyCombination.ModifierValue.UP
        );
    }
}