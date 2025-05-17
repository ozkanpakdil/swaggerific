package io.github.ozkanpakdil.swaggerific.ui.edit;

import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import io.github.ozkanpakdil.swaggerific.model.ShortcutModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class Shortcuts implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(Shortcuts.class);
    private static final String SHORTCUTS_PREFIX = "shortcut.";

    @FXML
    private TableView<ShortcutModel> tableShortcuts;

    @FXML
    private TableColumn<ShortcutModel, String> colAction;

    @FXML
    private TableColumn<ShortcutModel, String> colDescription;

    @FXML
    private TableColumn<ShortcutModel, String> colShortcut;

    @FXML
    private Button btnEdit;

    @FXML
    private Button btnReset;

    @FXML
    private Button btnSave;

    @FXML
    private Label lblStatus;

    private final ObservableList<ShortcutModel> shortcuts = FXCollections.observableArrayList();
    private final Map<String, KeyCodeCombination> defaultShortcuts = new HashMap<>();
    private final Preferences userPrefs = Preferences.userNodeForPackage(SwaggerApplication.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up table columns
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colShortcut.setCellValueFactory(new PropertyValueFactory<>("shortcut"));

        // Load shortcuts
        loadShortcuts();

        // Set table items
        tableShortcuts.setItems(shortcuts);

        // Add double-click event handler for the table
        tableShortcuts.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                handleEditShortcut(null);
            }
        });

        // Clear status label
        lblStatus.setText("");
    }

    /**
     * Loads shortcuts from the menu-bar.fxml file and from saved preferences.
     */
    private void loadShortcuts() {
        try {
            loadDefaultShortcuts();

            // Clear existing shortcuts
            shortcuts.clear();

            // Add shortcuts from the default map
            for (Map.Entry<String, KeyCodeCombination> entry : defaultShortcuts.entrySet()) {
                String action = entry.getKey();
                KeyCodeCombination defaultCombination = entry.getValue();

                // Check if there's a saved shortcut for this action
                String savedShortcutStr = userPrefs.get(SHORTCUTS_PREFIX + action, null);
                KeyCodeCombination combination = defaultCombination;

                if (savedShortcutStr != null && !savedShortcutStr.isEmpty()) {
                    KeyCodeCombination savedCombination = ShortcutModel.parseShortcut(savedShortcutStr);
                    if (savedCombination != null) {
                        combination = savedCombination;
                    }
                }

                // Create a description based on the action name
                String description = formatDescription(action);

                // Add to the list
                shortcuts.add(new ShortcutModel(action, description, combination));
            }
        } catch (Exception e) {
            log.error("Error loading shortcuts", e);
            showAlert("Error", "Failed to load shortcuts", e.getMessage());
        }
    }

    /**
     * Loads default shortcuts from all FXML files.
     */
    private void loadDefaultShortcuts() {
        try {
            // Clear existing defaults
            defaultShortcuts.clear();

            // Scan for FXML files in the resources directory
            scanForFxmlFiles("/io/github/ozkanpakdil/swaggerific");

            log.info("Loaded {} default shortcuts", defaultShortcuts.size());

        } catch (Exception e) {
            log.error("Error loading default shortcuts", e);
            showAlert("Error", "Failed to load default shortcuts", e.getMessage());
        }
    }

    /**
     * Recursively scans for FXML files in the specified directory and loads shortcuts from them.
     *
     * @param directory The directory to scan
     */
    private void scanForFxmlFiles(String directory) {
        try {
            // Get the URL of the directory
            URL directoryUrl = getClass().getResource(directory);
            if (directoryUrl == null) {
                log.debug("Directory not found: {}", directory);
                return;
            }

            // If the directory is a JAR file, we need to handle it differently
            if ("jar".equals(directoryUrl.getProtocol())) {
                // For JAR files, we need to scan the JAR file for FXML files
                // Get the JAR URL connection
                java.net.JarURLConnection jarConnection = (java.net.JarURLConnection) directoryUrl.openConnection();
                java.util.jar.JarFile jarFile = jarConnection.getJarFile();

                // Get the directory path within the JAR file
                String dirPath = directory;
                if (dirPath.startsWith("/")) {
                    dirPath = dirPath.substring(1); // Remove leading slash for JAR entries
                }

                log.info("Scanning JAR file for FXML files in directory: {}", dirPath);

                // Enumerate all entries in the JAR file
                java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                int fxmlFilesFound = 0;

                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // Check if the entry is an FXML file and is in the specified directory or its subdirectories
                    if (entryName.endsWith(".fxml") && entryName.startsWith(dirPath)) {
                        // Convert JAR entry path to resource path
                        String resourcePath = "/" + entryName;
                        log.info("Found FXML file in JAR: {}", resourcePath);
                        fxmlFilesFound++;

                        // Load shortcuts from the FXML file
                        loadShortcutsFromFile(resourcePath);
                    }
                }

                log.info("Found {} FXML files in JAR directory: {}", fxmlFilesFound, dirPath);

                // If no FXML files were found in the current directory, recursively scan subdirectories
                if (fxmlFilesFound == 0) {
                    log.info("No FXML files found in directory: {}. Scanning subdirectories...", dirPath);

                    // Get all subdirectories in the JAR file
                    java.util.Set<String> subdirectories = new java.util.HashSet<>();
                    entries = jarFile.entries();

                    while (entries.hasMoreElements()) {
                        java.util.jar.JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        // Check if the entry is in the specified directory
                        if (entryName.startsWith(dirPath) && entryName.length() > dirPath.length()) {
                            // Extract the next level subdirectory
                            String relativePath = entryName.substring(dirPath.length());
                            if (relativePath.startsWith("/")) {
                                relativePath = relativePath.substring(1);
                            }

                            int slashIndex = relativePath.indexOf('/');
                            if (slashIndex > 0) {
                                String subdir = dirPath + "/" + relativePath.substring(0, slashIndex);
                                subdirectories.add("/" + subdir);
                            }
                        }
                    }

                    // Recursively scan each subdirectory
                    for (String subdir : subdirectories) {
                        if (!directory.equals(subdir)) {
                            log.info("Scanning subdirectory: {}", subdir);
                            scanForFxmlFiles(subdir);
                        }
                    }
                }

                return;
            }

            // For file system access, we can use the File API
            File dir = new File(directoryUrl.toURI());
            if (dir.isDirectory()) {
                // Process all files in the directory
                for (File file : dir.listFiles()) {
                    if (file.isDirectory()) {
                        // Recursively scan subdirectories
                        scanForFxmlFiles(directory + "/" + file.getName());
                    } else if (file.getName().endsWith(".fxml")) {
                        // Load shortcuts from FXML files
                        loadShortcutsFromFile(directory + "/" + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error scanning for FXML files in directory: {}", directory, e);
        }
    }

    /**
     * Loads shortcuts from a specific FXML file.
     *
     * @param fxmlPath The path to the FXML file
     */
    private void loadShortcutsFromFile(String fxmlPath) {
        try (InputStream inputStream = getClass().getResourceAsStream(fxmlPath)) {
            if (inputStream == null) {
                log.debug("Could not find FXML file: {}", fxmlPath);
                return;
            }

            // Parse the XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            // Find all MenuItem elements
            NodeList menuItems = document.getElementsByTagName("MenuItem");
            for (int i = 0; i < menuItems.getLength(); i++) {
                Element menuItem = (Element) menuItems.item(i);

                // Check if it has an onAction attribute and an accelerator child
                String onAction = menuItem.getAttribute("onAction");
                if (onAction != null && !onAction.isEmpty()) {
                    if (onAction.startsWith("#")) {
                        onAction = onAction.substring(1);
                    }

                    NodeList accelerators = menuItem.getElementsByTagName("accelerator");
                    if (accelerators.getLength() > 0) {
                        Element accelerator = (Element) accelerators.item(0);
                        NodeList combinations = accelerator.getElementsByTagName("KeyCodeCombination");
                        if (combinations.getLength() > 0) {
                            Element combination = (Element) combinations.item(0);

                            String alt = combination.getAttribute("alt");
                            String control = combination.getAttribute("control");
                            String meta = combination.getAttribute("meta");
                            String shift = combination.getAttribute("shift");
                            String code = combination.getAttribute("code");

                            KeyCodeCombination keyCombination = createKeyCombination(alt, control, meta, shift, code);
                            if (keyCombination != null) {
                                defaultShortcuts.put(onAction, keyCombination);
                                log.debug("Loaded shortcut for {} from {}", onAction, fxmlPath);
                            }
                        }
                    }
                }
            }

            // Find all Button elements with mnemonics
            NodeList buttons = document.getElementsByTagName("Button");
            for (int i = 0; i < buttons.getLength(); i++) {
                Element button = (Element) buttons.item(i);

                String onAction = button.getAttribute("onAction");
                String text = button.getAttribute("text");

                if (onAction != null && !onAction.isEmpty() && text != null && text.contains("_")) {
                    if (onAction.startsWith("#")) {
                        onAction = onAction.substring(1);
                    }

                    int underscoreIndex = text.indexOf('_');
                    if (underscoreIndex >= 0 && underscoreIndex < text.length() - 1) {
                        char mnemonicChar = Character.toUpperCase(text.charAt(underscoreIndex + 1));
                        KeyCodeCombination keyCombination = new KeyCodeCombination(
                                KeyCode.getKeyCode(String.valueOf(mnemonicChar)),
                                KeyCombination.ModifierValue.UP,
                                KeyCombination.ModifierValue.UP,
                                KeyCombination.ModifierValue.DOWN,
                                KeyCombination.ModifierValue.UP,
                                KeyCombination.ModifierValue.UP
                        );
                        defaultShortcuts.put(onAction, keyCombination);
                        log.debug("Loaded mnemonic shortcut Alt+{} for {} from {}", mnemonicChar, onAction, fxmlPath);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error loading shortcuts from file: {}", fxmlPath, e);
        }
    }

    /**
     * Creates a KeyCodeCombination from the given attributes.
     */
    private KeyCodeCombination createKeyCombination(String alt, String control, String meta, String shift, String code) {
        try {
            return new KeyCodeCombination(
                    KeyCode.valueOf(code),
                    getModifierValue(shift),
                    getModifierValue(control),
                    getModifierValue(alt),
                    getModifierValue(meta),
                    KeyCombination.ModifierValue.UP
            );
        } catch (Exception e) {
            log.error("Error creating key combination", e);
            return null;
        }
    }

    /**
     * Converts a string modifier value to a KeyCombination.ModifierValue.
     */
    private KeyCombination.ModifierValue getModifierValue(String value) {
        if ("DOWN".equals(value)) {
            return KeyCombination.ModifierValue.DOWN;
        } else if ("ANY".equals(value)) {
            return KeyCombination.ModifierValue.ANY;
        } else {
            return KeyCombination.ModifierValue.UP;
        }
    }

    /**
     * Formats a description based on the action name.
     */
    private String formatDescription(String action) {
        // Convert camelCase to words with spaces
        String result = action.replaceAll("([a-z])([A-Z])", "$1 $2");

        // Capitalize first letter
        if (!result.isEmpty()) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }

        return result;
    }

    /**
     * Handles the Edit button click.
     */
    @FXML
    public void handleEditShortcut(ActionEvent event) {
        ShortcutModel selected = tableShortcuts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "No Shortcut Selected", "Please select a shortcut to edit.");
            return;
        }

        // Show a dialog to edit the shortcut
        TextInputDialog dialog = new TextInputDialog(selected.getShortcut());
        dialog.setTitle("Edit Shortcut");
        dialog.setHeaderText("Edit Shortcut for " + selected.getAction());
        dialog.setContentText("Shortcut:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(shortcutStr -> {
            try {
                KeyCodeCombination combination = ShortcutModel.parseShortcut(shortcutStr);
                if (combination != null) {
                    selected.setKeyCombination(combination);
                } else {
                    showAlert("Invalid Shortcut", "Invalid Shortcut Format",
                            "Please use format like 'Ctrl+S' or 'Alt+Shift+X'.");
                }
            } catch (Exception e) {
                log.error("Error parsing shortcut", e);
                showAlert("Error", "Failed to parse shortcut", e.getMessage());
            }
        });
    }

    /**
     * Handles the Reset to Default button click.
     */
    @FXML
    public void handleResetShortcuts(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Shortcuts");
        alert.setHeaderText("Reset All Shortcuts to Default");
        alert.setContentText("Are you sure you want to reset all shortcuts to their default values?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Reset all shortcuts to default
            for (ShortcutModel shortcut : shortcuts) {
                KeyCodeCombination defaultCombination = defaultShortcuts.get(shortcut.getAction());
                if (defaultCombination != null) {
                    shortcut.setKeyCombination(defaultCombination);
                }
            }

            // Clear saved preferences
            clearSavedShortcuts();

            // Update status label
            lblStatus.setText("All shortcuts have been reset to their default values.");
            lblStatus.setStyle("-fx-text-fill: green;");
        }
    }

    /**
     * Handles the Save button click.
     */
    @FXML
    public void handleSaveShortcuts(ActionEvent event) {
        try {
            // Save all shortcuts to preferences
            for (ShortcutModel shortcut : shortcuts) {
                String action = shortcut.getAction();
                String shortcutStr = shortcut.getShortcut();

                // Check if it's different from the default
                KeyCodeCombination defaultCombination = defaultShortcuts.get(action);
                String defaultShortcutStr = ShortcutModel.formatKeyCombination(defaultCombination);

                if (!shortcutStr.equals(defaultShortcutStr)) {
                    // Save only if different from default
                    userPrefs.put(SHORTCUTS_PREFIX + action, shortcutStr);
                } else {
                    // Remove if same as default
                    userPrefs.remove(SHORTCUTS_PREFIX + action);
                }
            }

            // Apply shortcuts immediately to all open windows
            SwaggerApplication.getInstance().applyCustomShortcutsToAllWindows();

            // Update status label
            lblStatus.setText("All shortcuts have been saved and applied immediately.");
            lblStatus.setStyle("-fx-text-fill: green;");
        } catch (Exception e) {
            log.error("Error saving shortcuts", e);
            // Update status label with error
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }

    /**
     * Clears all saved shortcuts from preferences.
     */
    private void clearSavedShortcuts() {
        try {
            // Get all keys
            String[] keys = userPrefs.keys();

            // Remove all shortcut keys
            for (String key : keys) {
                if (key.startsWith(SHORTCUTS_PREFIX)) {
                    userPrefs.remove(key);
                }
            }
        } catch (Exception e) {
            log.error("Error clearing saved shortcuts", e);
        }
    }

    /**
     * Shows an alert dialog.
     */
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
