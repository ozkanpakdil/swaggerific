package io.github.ozkanpakdil.swaggerific.ui.edit;

import io.github.ozkanpakdil.swaggerific.tools.HttpUtility;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpRequest;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpService;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpServiceImpl;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import io.github.ozkanpakdil.swaggerific.ui.textfx.JavaScriptColorize;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.PathItem;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for the Pre-request Script tab in the request panel.
 * Handles JavaScript execution before sending the main HTTP request.
 */
public class PreRequestScriptController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(PreRequestScriptController.class);

    @FXML
    private CodeArea codePreRequestScript;

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private final ScriptEngine scriptEngine = initializeScriptEngine();

    private ScriptEngine initializeScriptEngine() {
        ScriptEngine engine = null;

        // Try different engine names that GraalVM might use
        String[] engineNames = {"graal.js", "js", "JavaScript", "javascript", "ECMAScript", "ecmascript"};

        for (String engineName : engineNames) {
            engine = scriptEngineManager.getEngineByName(engineName);
            if (engine != null) {
                log.info("Successfully initialized JavaScript engine: {}", engineName);
                return engine;
            }
        }

        log.error("No JavaScript engine found! Available engines:");
        scriptEngineManager.getEngineFactories().forEach(factory -> {
            log.error("  Engine: {} ({}), Language: {} ({}), Extensions: {}", 
                factory.getEngineName(), factory.getEngineVersion(),
                factory.getLanguageName(), factory.getLanguageVersion(),
                factory.getExtensions());
        });

        return null;
    }
    private final HttpService httpService = new HttpServiceImpl();
    private final HttpUtility httpUtility = new HttpUtility();

    // For syntax highlighting
    private final JavaScriptColorize javaScriptColorize = new JavaScriptColorize();

    // Store for variables that can be accessed across script executions
    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    // Callback to be called when script execution is complete
    private Runnable onScriptExecutionComplete;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up code editor with JavaScript syntax highlighting
        setupCodeEditor();

        // Initialize with sample code
        codePreRequestScript.replaceText(
                "// Example pre-request script\n" +
                "// You can use the pm object to access variables and send requests\n\n" +
                "// Set a variable\n" +
                "// pm.variables.set(\"variable_key\", \"variable_value\");\n\n" +
                "// Get a variable\n" +
                "// var value = pm.variables.get(\"variable_key\");\n" +
                "// console.log(\"Variable value: \" + value);\n\n" +
                "// Send a request\n" +
                "/*pm.sendRequest(\"https://postman-echo.com/get\", function (err, response) {\n" +
                "    if (err) {\n" +
                "        console.log(err);\n" +
                "    } else {\n" +
                "        console.log(response.json());\n" +
                "        // Set a variable from the response\n" +
                "        pm.variables.set(\"response_data\", response.json().url);\n" +
                "    }\n" +
                "});*/"
        );
    }

    /**
     * Sets up the code editor with JavaScript syntax highlighting
     */
    private void setupCodeEditor() {
        // Add line numbers
        codePreRequestScript.setParagraphGraphicFactory(LineNumberFactory.get(codePreRequestScript));

        // Add CSS for syntax highlighting
        codePreRequestScript.getStylesheets().add(
                MainController.class.getResource("/css/javascript-highlighting.css").toString());

        // Enable text wrapping and line highlighting
        codePreRequestScript.setWrapText(true);
        codePreRequestScript.setLineHighlighterOn(true);

        // Apply JavaScript syntax highlighting
        codePreRequestScript.textProperty().addListener(
                (obs, oldText, newText) -> codePreRequestScript.setStyleSpans(
                        0, javaScriptColorize.computeHighlighting(newText)));
    }

    /**
     * Sets a callback to be called when script execution is complete
     * 
     * @param callback the callback to call
     */
    public void setOnScriptExecutionComplete(Runnable callback) {
        this.onScriptExecutionComplete = callback;
    }

    /**
     * Executes the pre-request script and applies any changes to the provided headers map
     * 
     * @param headers The headers map to update with script-modified headers
     * @return CompletableFuture that completes when script execution is done
     */
    public CompletableFuture<Void> executeScript(Map<String, String> headers) {
        // Run script execution in a separate thread to avoid JavaFX threading issues
        return CompletableFuture.runAsync(() -> {
            try {
                // Check if script engine is available
                if (scriptEngine == null) {
                    String errorMsg = "JavaScript engine is not available. Please ensure GraalVM JavaScript engine is properly configured.";
                    log.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }

                String script = getScript();
                if (script == null || script.trim().isEmpty()) {
                    return;
                }

                // Create bindings for the script
                SimpleBindings bindings = new SimpleBindings();

                // Create the pm object with variables and request functions
                Map<String, Object> pm = new HashMap<>();

                // Variables object - create JavaScript-friendly functions
                Map<String, Object> pmVariables = new HashMap<>();
                pmVariables.put("get", (java.util.function.Function<String, Object>) key -> {
                    Object value = variables.get(key);
                    log.debug("Getting variable: {} = {}", key, value);
                    return value;
                });
                pmVariables.put("set", (java.util.function.BiConsumer<String, Object>) (key, value) -> {
                    log.debug("Setting variable: {} = {}", key, value);
                    variables.put(key, value);
                });
                pm.put("variables", pmVariables);

                // Console object for logging
                Map<String, Object> console = new HashMap<>();
                console.put("log", (java.util.function.Consumer<Object>) message -> 
                    log.info("Script console.log: {}", message));
                console.put("error", (java.util.function.Consumer<Object>) message -> 
                    log.error("Script console.error: {}", message));
                console.put("warn", (java.util.function.Consumer<Object>) message -> 
                    log.warn("Script console.warn: {}", message));

                // Put console directly in bindings (global scope)
                bindings.put("console", console);

                // sendRequest function - handle JavaScript callbacks without casting to BiConsumer
                pm.put("sendRequest", (java.util.function.BiConsumer<String, Object>) 
                    (url, callback) -> {
                        try {
                            HttpResponse response = httpUtility.sendRequest(url, PathItem.HttpMethod.GET);

                            // Create response object for the callback
                            Map<String, Object> responseObj = new HashMap<>();
                            responseObj.put("status", response.statusCode());
                            responseObj.put("body", response.body());
                            responseObj.put("headers", response.headers());

                            // Add json method to response object
                            responseObj.put("json", (java.util.function.Supplier<Object>) () -> {
                                try {
                                    return Json.mapper().readValue(response.body(), Map.class);
                                } catch (Exception e) {
                                    log.error("Error parsing JSON response: {}", e.getMessage());
                                    return Map.of("error", "Failed to parse JSON: " + e.getMessage());
                                }
                            });

                            // Call the callback with null error and response
                            // Use reflection to invoke the callback function without casting
                            try {
                                if (callback instanceof javax.script.Invocable) {
                                    ((javax.script.Invocable) callback).invokeMethod(null, "call", null, responseObj);
                                } else {
                                    // For ScriptObjectMirror or other JavaScript function objects
                                    java.lang.reflect.Method callMethod = callback.getClass().getMethod("call", Object.class, Object[].class);
                                    callMethod.invoke(callback, null, new Object[]{null, responseObj});
                                }
                            } catch (Exception e) {
                                log.error("Error invoking JavaScript callback: {}", e.getMessage());
                            }
                        } catch (Exception e) {
                            log.error("Error sending request from script: {}", e.getMessage());
                            try {
                                if (callback instanceof javax.script.Invocable) {
                                    ((javax.script.Invocable) callback).invokeMethod(null, "call", e, null);
                                } else {
                                    // For ScriptObjectMirror or other JavaScript function objects
                                    java.lang.reflect.Method callMethod = callback.getClass().getMethod("call", Object.class, Object[].class);
                                    callMethod.invoke(callback, null, new Object[]{e, null});
                                }
                            } catch (Exception callbackError) {
                                log.error("Error invoking JavaScript callback with error: {}", callbackError.getMessage());
                            }
                        }
                    }
                );

                // Add headers object to allow script to modify request headers
                Map<String, Object> request = new HashMap<>();
                request.put("headers", headers);
                pm.put("request", request);

                // Put pm object in bindings
                bindings.put("pm", pm);

                // Also put pm properties directly in bindings for easier access
                bindings.put("pmVariables", pmVariables);
                bindings.put("pmRequest", request);
                bindings.put("pmHeaders", headers);

                // Add helper object for header manipulation since GraalVM doesn't allow direct Map modification
                HeaderHelper headerHelper = new HeaderHelper(headers);
                bindings.put("headerHelper", headerHelper);

                // Debug: Add a simple test to see what's available
                log.info("Bindings keys: {}", bindings.keySet());
                log.info("PM object: {}", pm);
                log.info("PM request object: {}", pm.get("request"));
                log.info("Headers object: {}", headers);

                // Execute the script
                scriptEngine.eval(script, bindings);

                // Debug: Check headers after script execution
                log.info("Headers after script execution: {}", headers);

                // Headers are already updated directly since pmHeaders is a reference to the same map
                // No need to copy back as the script modifies the original headers map directly

                // Notify that script execution is complete
                if (onScriptExecutionComplete != null) {
                    onScriptExecutionComplete.run();
                }
            } catch (ScriptException e) {
                log.error("Error executing pre-request script: {}", e.getMessage());
                // Don't show error dialog in separate thread - just log the error
                throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Shows an error dialog for script execution errors
     */
    private void showScriptError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, 
                "Error executing pre-request script: " + e.getMessage(), 
                ButtonType.OK);
        alert.setTitle("Script Error");
        alert.setHeaderText("JavaScript Error");
        alert.showAndWait();
    }

    /**
     * Gets the current script text
     */
    public String getScript() {
        return codePreRequestScript.getText();
    }

    /**
     * Sets the script text
     */
    public void setScript(String script) {
        codePreRequestScript.replaceText(script);
    }

    /**
     * Gets the variables map
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * Inserts a snippet to get a variable
     */
    @FXML
    public void insertGetVariableSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = "var value = pm.variables.get(\"variable_name\");\n" +
                "console.log(\"Variable value: \" + value);\n";
        codePreRequestScript.insertText(position, snippet);

        // Ensure syntax highlighting is applied to the inserted text
        refreshSyntaxHighlighting();
    }

    /**
     * Inserts a snippet to set a variable
     */
    @FXML
    public void insertSetVariableSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = "pm.variables.set(\"variable_name\", \"variable_value\");\n";
        codePreRequestScript.insertText(position, snippet);

        // Ensure syntax highlighting is applied to the inserted text
        refreshSyntaxHighlighting();
    }

    /**
     * Inserts a snippet to send a request
     */
    @FXML
    public void insertSendRequestSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = "pm.sendRequest(\"https://example.com/api\", function (err, response) {\n" +
                "    if (err) {\n" +
                "        console.log(err);\n" +
                "    } else {\n" +
                "        console.log(response.json());\n" +
                "        // Set a variable from the response\n" +
                "        pm.variables.set(\"response_data\", response.json().data);\n" +
                "    }\n" +
                "});\n";
        codePreRequestScript.insertText(position, snippet);

        // Ensure syntax highlighting is applied to the inserted text
        refreshSyntaxHighlighting();
    }

    /**
     * Inserts a snippet to modify headers
     */
    @FXML
    public void insertModifyHeadersSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = "// Add or modify a request header\n" +
                "pm.request.headers[\"Custom-Header\"] = \"Header-Value\";\n";
        codePreRequestScript.insertText(position, snippet);

        // Ensure syntax highlighting is applied to the inserted text
        refreshSyntaxHighlighting();
    }

    /**
     * Inserts a snippet to log a message
     */
    @FXML
    public void insertLogSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = "console.log(\"Your message here\");\n";
        codePreRequestScript.insertText(position, snippet);

        // Ensure syntax highlighting is applied to the inserted text
        refreshSyntaxHighlighting();
    }

    /**
     * Refreshes the syntax highlighting for the entire code area
     */
    private void refreshSyntaxHighlighting() {
        codePreRequestScript.setStyleSpans(0, javaScriptColorize.computeHighlighting(codePreRequestScript.getText()));
    }

    /**
     * Inserts a complete example snippet
     */
    @FXML
    public void insertCompleteExampleSnippet() {
        codePreRequestScript.replaceText(
                "// Example pre-request script\n" +
                "// You can use the pm object to access variables and send requests\n\n" +
                "// Set a variable\n" +
                "pm.variables.set(\"api_key\", \"your-api-key\");\n\n" +
                "// Get a variable\n" +
                "var apiKey = pm.variables.get(\"api_key\");\n" +
                "console.log(\"API Key: \" + apiKey);\n\n" +
                "// Add a custom header\n" +
                "pm.request.headers[\"X-API-Key\"] = apiKey;\n\n" +
                "// Send a request to get authentication token\n" +
                "pm.sendRequest(\"https://auth.example.com/token\", function (err, response) {\n" +
                "    if (err) {\n" +
                "        console.log(\"Error getting token: \" + err);\n" +
                "    } else {\n" +
                "        var token = response.json().token;\n" +
                "        console.log(\"Got token: \" + token);\n" +
                "        \n" +
                "        // Store the token in a variable for the main request\n" +
                "        pm.variables.set(\"auth_token\", token);\n" +
                "        \n" +
                "        // Add the token to the Authorization header\n" +
                "        pm.request.headers[\"Authorization\"] = \"Bearer \" + token;\n" +
                "    }\n" +
                "});\n"
        );

        // Ensure syntax highlighting is applied to the new text
        refreshSyntaxHighlighting();
    }
}

/**
 * Helper class for header manipulation from JavaScript
 */
class HeaderHelper {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HeaderHelper.class);
    private final Map<String, String> headers;

    public HeaderHelper(Map<String, String> headers) {
        this.headers = headers;
    }

    public void set(String key, String value) {
        log.debug("Setting header: {} = {}", key, value);
        headers.put(key, value);
    }

    public String get(String key) {
        String value = headers.get(key);
        log.debug("Getting header: {} = {}", key, value);
        return value;
    }
}

/**
 * Helper class for variable manipulation from JavaScript
 */
class VariablesHelper {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VariablesHelper.class);
    private final Map<String, Object> variables;

    public VariablesHelper(Map<String, Object> variables) {
        this.variables = variables;
    }

    public void set(String key, Object value) {
        log.debug("Setting variable: {} = {}", key, value);
        variables.put(key, value);
    }

    public Object get(String key) {
        Object value = variables.get(key);
        log.debug("Getting variable: {} = {}", key, value);
        return value;
    }
}
