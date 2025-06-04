package io.github.ozkanpakdil.swaggerific;

import atlantafx.base.theme.PrimerLight;
import io.github.ozkanpakdil.swaggerific.animation.Preloader;
import io.github.ozkanpakdil.swaggerific.model.ShortcutModel;
import io.github.ozkanpakdil.swaggerific.tools.ProxySettings;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpServiceImpl;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

import static io.github.ozkanpakdil.swaggerific.ui.edit.General.FONT_SIZE;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.SELECTED_FONT;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.STAGE_HEIGHT;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.STAGE_WIDTH;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.STAGE_X;
import static io.github.ozkanpakdil.swaggerific.ui.edit.General.STAGE_Y;

public class SwaggerApplication extends Application {
    static SwaggerApplication instance;
    private Stage primaryStage;
    Preferences userPrefs = Preferences.userNodeForPackage(getClass());
    private static final Logger log = LoggerFactory.getLogger(SwaggerApplication.class);
    private static final String SHORTCUTS_PREFIX = "shortcut.";

    @Override
    public void start(Stage stage) throws IOException {
        instance = this;
        this.primaryStage = stage;
        String fontSize = userPrefs.get(FONT_SIZE, ".93em");
        String selectedFont = userPrefs.get(SELECTED_FONT, "Verdana");

        // Initialize proxy settings
        initializeProxySettings();

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        loadingWindowLookAndLocation();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-view.fxml"));
        Parent root = fxmlLoader.load();
        log.info("font size:" + fontSize);
        log.info("font family:" + selectedFont);
        MainController mainController = fxmlLoader.getController();
        mainController.onOpening();

        // Apply custom shortcuts
        applyCustomShortcuts(root);

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Swaggerific");
        stage.getIcons().add(new Image(Objects.requireNonNull(SwaggerApplication.class.getResourceAsStream("/applogo.png"))));
        stage.setScene(scene);
        stage.setOnHidden(e -> mainController.onClose());
        stage.show();
        //TODO this size change is not working, investigate. // below font change is not working on application start :(
        root.setStyle("-fx-font-size:" + fontSize + ";");
        root.setStyle("-fx-font-family:'" + selectedFont + "';");
        mainController.getTopPane().getScene().getRoot().setStyle("-fx-font-size:" + fontSize + ";");
        mainController.getTopPane().getScene().getRoot().setStyle("-fx-font-family:'" + selectedFont + "';");
    }

    public static SwaggerApplication getInstance() {
        return instance;
    }

    private void loadingWindowLookAndLocation() {
        primaryStage.setX(userPrefs.getDouble(STAGE_X, 0));
        primaryStage.setY(userPrefs.getDouble(STAGE_Y, 0));
        primaryStage.setWidth(userPrefs.getDouble(STAGE_WIDTH, 800));
        primaryStage.setHeight(userPrefs.getDouble(STAGE_HEIGHT, 600));
    }

    /**
     * Initializes proxy settings for the application. This sets up proxy authentication and system properties if needed.
     * This method can be called to reinitialize proxy settings after they have been changed.
     */
    public static void initializeProxySettings() {
        log.info("Initializing proxy settings...");

        if (ProxySettings.useSystemProxy()) {
            System.setProperty("java.net.useSystemProxies", "true");
            log.info("Using system proxy settings");
        } else {
            System.setProperty("java.net.useSystemProxies", "false");

            String proxyHost = ProxySettings.getProxyServer();
            int proxyPort = ProxySettings.getProxyPort();

            if (proxyHost != null && !proxyHost.isEmpty()) {
                log.info("Custom proxy configured: {}:{}", proxyHost, proxyPort);

                // Install JVM-wide ProxySelector
                ProxySelector.setDefault(new ProxySelector() {
                    private final ProxySelector defaultSelector = ProxySelector.getDefault();

                    @Override
                    public List<java.net.Proxy> select(URI uri) {
                        if (ProxySettings.shouldBypassProxy(uri.getHost())) {
                            return Collections.singletonList(java.net.Proxy.NO_PROXY);
                        }
                        java.net.Proxy proxy = new java.net.Proxy(
                                java.net.Proxy.Type.HTTP,
                                new InetSocketAddress(proxyHost, proxyPort)
                        );
                        return Collections.singletonList(proxy);
                    }

                    @Override
                    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                        log.error("Proxy connection failed for {}: {}", uri, ioe.getMessage());
                        if (defaultSelector != null) {
                            defaultSelector.connectFailed(uri, sa, ioe);
                        }
                    }
                });
            } else {
                // Clear all proxy settings
                ProxySelector.setDefault(null);
                log.info("No proxy configured");
            }
        }

        // Recreate all HttpClient instances to apply new proxy settings
        HttpServiceImpl.recreateAllHttpClients();

        log.info("Proxy settings reinitialized successfully");
    }

    /**
     * Applies custom shortcuts from preferences to menu items and other controls.
     *
     * @param root The root node of the scene graph
     */
    private void applyCustomShortcuts(Parent root) {
        try {
            // Find all controls with shortcuts in the scene graph
            findAndApplyShortcuts(root);
        } catch (Exception e) {
            log.error("Error applying custom shortcuts", e);
        }
    }

    /**
     * Static method to apply custom shortcuts to a scene. This can be called from anywhere to refresh shortcuts.
     *
     * @param scene The scene to apply shortcuts to
     */
    public static void applyCustomShortcutsToScene(Scene scene) {
        if (scene == null) {
            log.error("Cannot apply shortcuts to null scene");
            return;
        }

        javafx.application.Platform.runLater(() -> {
            try {
                // Get the root node of the scene
                Parent root = scene.getRoot();
                if (root == null) {
                    log.error("Scene has no root node");
                    return;
                }

                // Create a temporary instance to access non-static methods
                SwaggerApplication tempInstance = new SwaggerApplication();
                tempInstance.findAndApplyShortcuts(root);
            } catch (Exception e) {
                log.error("Error applying custom shortcuts to scene", e);
            }
        });
    }

    /**
     * Static method to apply custom shortcuts to all open windows. This can be called from anywhere to refresh shortcuts.
     */
    public void applyCustomShortcutsToAllWindows() {
        if (primaryStage == null) {
            log.error("Primary stage is not initialized");
            return;
        }

        try {
            // Apply shortcuts to the primary stage
            applyCustomShortcutsToScene(primaryStage.getScene());

            // Apply shortcuts to all other stages
            for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                if (window instanceof javafx.stage.Stage stage && stage != primaryStage) {
                    applyCustomShortcutsToScene(stage.getScene());
                }
            }
        } catch (Exception e) {
            log.error("Error applying custom shortcuts to all windows", e);
        }
    }

    /**
     * Recursively finds all controls with shortcuts in the scene graph and applies custom shortcuts.
     *
     * @param parent The parent node to search
     */
    private void findAndApplyShortcuts(Parent parent) {
        // Process all children
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            // If it's a MenuBar, process its menus and menu items
            if (node instanceof javafx.scene.control.MenuBar menuBar) {
                for (javafx.scene.control.Menu menu : menuBar.getMenus()) {
                    processMenu(menu);
                }
            }

            // If it's a Button, apply custom shortcuts
            if (node instanceof javafx.scene.control.Button button) {
                applyCustomShortcutToButton(button);
            }

            // If it's a Parent, recursively process its children
            if (node instanceof Parent childParent) {
                findAndApplyShortcuts(childParent);
            }
        }
    }

    /**
     * Processes a menu and its items, applying custom shortcuts.
     *
     * @param menu The menu to process
     */
    private void processMenu(javafx.scene.control.Menu menu) {
        // Process all items in the menu
        for (MenuItem item : menu.getItems()) {
            // If it's a MenuItem, apply custom shortcuts
            applyCustomShortcutToMenuItem(item);

            // If it's a Menu, recursively process its items
            if (item instanceof javafx.scene.control.Menu subMenu) {
                processMenu(subMenu);
            }
        }
    }

    /**
     * Applies a custom shortcut to a menu item if one exists in preferences.
     *
     * @param menuItem The menu item to apply the shortcut to
     */
    private void applyCustomShortcutToMenuItem(MenuItem menuItem) {
        try {
            // Get the action name from the onAction property
            String onAction = getActionNameFromMenuItem(menuItem);
            if (onAction == null || onAction.isEmpty()) {
                return;
            }

            // Check if there's a custom shortcut for this action
            String customShortcutStr = userPrefs.get(SHORTCUTS_PREFIX + onAction, null);
            if (customShortcutStr == null || customShortcutStr.isEmpty()) {
                return;
            }

            // Parse the custom shortcut
            KeyCodeCombination customCombination = ShortcutModel.parseShortcut(customShortcutStr);
            if (customCombination != null) {
                // Apply the custom shortcut
                menuItem.setAccelerator(customCombination);
                log.info("Applied custom shortcut {} to {}", customShortcutStr, onAction);
            }
        } catch (Exception e) {
            log.error("Error applying custom shortcut to menu item", e);
        }
    }

    /**
     * Gets the action name from a menu item.
     *
     * @param menuItem The menu item
     * @return The action name, or null if not found
     */
    private String getActionNameFromMenuItem(MenuItem menuItem) {
        try {
            // Get the onAction property using reflection
            java.lang.reflect.Method getOnActionMethod = MenuItem.class.getMethod("getOnAction");
            Object onActionObject = getOnActionMethod.invoke(menuItem);

            if (onActionObject == null) {
                return null;
            }

            // Get the method name from the event handler
            String onActionStr = onActionObject.toString();

            // Extract the method name from the event handler string
            // The format is typically something like "EventHandler [handler: controllerClass@hashCode::methodName]"
            int methodStartIndex = onActionStr.lastIndexOf("::");
            if (methodStartIndex == -1) {
                return null;
            }

            String methodName = onActionStr.substring(methodStartIndex + 2);
            // Remove any closing brackets or other characters
            methodName = methodName.replaceAll("[^a-zA-Z0-9]", "");

            return methodName;
        } catch (Exception e) {
            log.debug("Error getting action name from menu item: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Applies a custom shortcut to a button if one exists in preferences.
     *
     * @param button The button to apply the shortcut to
     */
    private void applyCustomShortcutToButton(javafx.scene.control.Button button) {
        try {
            // Get the action name from the onAction property
            String onAction = getActionNameFromButton(button);
            if (onAction == null || onAction.isEmpty()) {
                return;
            }

            // Check if there's a custom shortcut for this action
            String customShortcutStr = userPrefs.get(SHORTCUTS_PREFIX + onAction, null);
            if (customShortcutStr == null || customShortcutStr.isEmpty()) {
                return;
            }

            // Parse the custom shortcut
            KeyCodeCombination customCombination = ShortcutModel.parseShortcut(customShortcutStr);
            if (customCombination != null) {
                // Apply the custom shortcut by setting the mnemonic parsing and text
                button.setMnemonicParsing(true);
                String text = button.getText();
                if (text != null && !text.isEmpty()) {
                    // Remove any existing underscores (mnemonics)
                    text = text.replace("_", "");

                    // Get the key code name
                    String keyCodeName = customCombination.getCode().getName();

                    // Log the key code and button text for debugging
                    log.debug("Applying shortcut with key code {} to button text '{}'", keyCodeName, text);

                    // Try different strategies to find a suitable character for the mnemonic
                    int index = findSuitableCharacterIndex(text, keyCodeName);

                    if (index >= 0) {
                        // Insert underscore before the character to make it a mnemonic
                        String newText = text.substring(0, index) + "_" + text.substring(index);
                        button.setText(newText);
                        log.info("Applied custom shortcut {} to button '{}' (action: {})",
                                customShortcutStr, newText, onAction);
                    } else {
                        // If no suitable character is found, append the key code to the button text
                        String newText = text + " (" + keyCodeName + ")";
                        button.setText(newText);

                        // Try again to find a suitable character in the new text
                        index = findSuitableCharacterIndex(newText, keyCodeName);
                        if (index >= 0) {
                            // Insert underscore before the character to make it a mnemonic
                            newText = newText.substring(0, index) + "_" + newText.substring(index);
                            button.setText(newText);
                            log.info("Applied custom shortcut {} to button '{}' with appended key code (action: {})",
                                    customShortcutStr, newText, onAction);
                        } else {
                            log.warn(
                                    "Could not find a suitable character for mnemonic in button text '{}', even after appending key code",
                                    text);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error applying custom shortcut to button", e);
        }
    }

    /**
     * Finds a suitable character index in the text for a mnemonic based on the key code name.
     *
     * @param text The text to search in
     * @param keyCodeName The key code name to search for
     * @return The index of a suitable character, or -1 if none is found
     */
    private int findSuitableCharacterIndex(String text, String keyCodeName) {
        // Strategy 1: Try to find the first character of the key code in the text
        if (!keyCodeName.isEmpty()) {
            char keyChar = keyCodeName.charAt(0);
            int index = text.toLowerCase().indexOf(Character.toLowerCase(keyChar));
            if (index >= 0) {
                log.debug("Found first character '{}' of key code at position {}", keyChar, index);
                return index;
            }
            log.debug("First character '{}' of key code not found in text", keyChar);
        }

        // Strategy 2: Try to find any character from the key code name in the text
        for (int i = 0; i < keyCodeName.length(); i++) {
            char c = keyCodeName.charAt(i);
            int pos = text.toLowerCase().indexOf(Character.toLowerCase(c));
            if (pos >= 0) {
                log.debug("Found character '{}' from key code at position {}", c, pos);
                return pos;
            }
        }
        log.debug("No character from key code found in text");

        // Strategy 3: Use the first letter or digit in the text
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) {
                log.debug("Using first letter/digit '{}' at position {}", text.charAt(i), i);
                return i;
            }
        }

        log.debug("No letter or digit found in text");
        return -1;
    }

    /**
     * Gets the action name from a button.
     *
     * @param button The button
     * @return The action name, or null if not found
     */
    private String getActionNameFromButton(javafx.scene.control.Button button) {
        try {
            // Get the onAction property using reflection
            java.lang.reflect.Method getOnActionMethod = javafx.scene.control.ButtonBase.class.getMethod("getOnAction");
            Object onActionObject = getOnActionMethod.invoke(button);

            if (onActionObject == null) {
                return null;
            }

            // Get the method name from the event handler
            String onActionStr = onActionObject.toString();

            // Extract the method name from the event handler string
            // The format is typically something like "EventHandler [handler: controllerClass@hashCode::methodName]"
            int methodStartIndex = onActionStr.lastIndexOf("::");
            if (methodStartIndex == -1) {
                return null;
            }

            String methodName = onActionStr.substring(methodStartIndex + 2);
            // Remove any closing brackets or other characters
            methodName = methodName.replaceAll("[^a-zA-Z0-9]", "");

            return methodName;
        } catch (Exception e) {
            log.debug("Error getting action name from button: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void stop() {
        userPrefs.putDouble(STAGE_X, primaryStage.getX());
        userPrefs.putDouble(STAGE_Y, primaryStage.getY());
        userPrefs.putDouble(STAGE_WIDTH, primaryStage.getWidth());
        userPrefs.putDouble(STAGE_HEIGHT, primaryStage.getHeight());
    }

    public static void main(String[] args) {
        System.setProperty("javafx.preloader", Preloader.class.getName());
        if (log.isDebugEnabled())
            System.setProperty("jdk.httpclient.HttpClient.log", "all");
        launch();
    }
}
