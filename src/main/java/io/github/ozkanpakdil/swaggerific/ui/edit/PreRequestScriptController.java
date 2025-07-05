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

                // Create JavaScript objects for variables and headers
                // Convert Java variables to JavaScript object
                StringBuilder jsVariables = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    if (!first) jsVariables.append(",");
                    jsVariables.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                    first = false;
                }
                jsVariables.append("}");

                // Convert Java headers to JavaScript object
                StringBuilder jsHeaders = new StringBuilder("{");
                first = true;
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (!first) jsHeaders.append(",");
                    jsHeaders.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                    first = false;
                }
                jsHeaders.append("}");

                // Debug: Add a simple test to see what's available
                log.info("Bindings keys: {}", bindings.keySet());
                log.info("Headers object: {}", headers);

                // Create pm object structure in JavaScript using pure JavaScript objects
                String pmSetupScript = 
                    "var __jsVariables = " + jsVariables.toString() + ";" +
                    "var __jsHeaders = " + jsHeaders.toString() + ";" +
                    "var console = {" +
                    "  log: function(message) { /* JavaScript console.log - message: */ }," +
                    "  error: function(message) { /* JavaScript console.error - message: */ }," +
                    "  warn: function(message) { /* JavaScript console.warn - message: */ }" +
                    "};" +
                    "var pm = {" +
                    "  variables: {" +
                    "    get: function(key) { return __jsVariables[key]; }," +
                    "    set: function(key, value) { __jsVariables[key] = value; }" +
                    "  }," +
                    "  request: {" +
                    "    headers: __jsHeaders" +
                    "  }," +
                    "  sendRequest: function(url, callback) { console.log('sendRequest called with URL: ' + url); }" +
                    "};";

                // Execute the pm setup script first
                try {
                    log.info("Executing pm setup script: {}", pmSetupScript);
                    scriptEngine.eval(pmSetupScript, bindings);
                    log.info("PM setup script executed successfully");

                    // Test if pm object was created
                    Object pmTest = scriptEngine.eval("typeof pm", bindings);
                    log.info("PM object type: {}", pmTest);

                    if ("object".equals(pmTest)) {
                        Object pmVarTest = scriptEngine.eval("typeof pm.variables", bindings);
                        log.info("PM variables type: {}", pmVarTest);
                    }
                } catch (Exception e) {
                    log.error("Error executing pm setup script: {}", e.getMessage(), e);
                    throw e;
                }

                // Execute the script
                scriptEngine.eval(script, bindings);

                // Sync JavaScript objects back to Java objects
                try {
                    // Get the JavaScript variables object and sync back to Java
                    Object jsVarResult = scriptEngine.eval("__jsVariables", bindings);
                    log.info("JavaScript variables result type: {}, value: {}", jsVarResult.getClass().getName(), jsVarResult);

                    if (jsVarResult instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> jsVarMap = (java.util.Map<String, Object>) jsVarResult;
                        variables.clear();
                        variables.putAll(jsVarMap);
                        log.info("Synced variables from JavaScript: {}", variables);
                    } else {
                        // Try to access as object properties using eval
                        String jsVarKeys = (String) scriptEngine.eval("Object.keys(__jsVariables).join(',')", bindings);
                        log.info("JavaScript variable keys: {}", jsVarKeys);

                        if (jsVarKeys != null && !jsVarKeys.isEmpty()) {
                            variables.clear();
                            for (String key : jsVarKeys.split(",")) {
                                if (!key.trim().isEmpty()) {
                                    Object value = scriptEngine.eval("__jsVariables['" + key.trim() + "']", bindings);
                                    variables.put(key.trim(), value);
                                    log.info("Synced variable: {} = {}", key.trim(), value);
                                }
                            }
                        }
                    }

                    // Get the JavaScript headers object and sync back to Java
                    Object jsHeaderResult = scriptEngine.eval("__jsHeaders", bindings);
                    log.info("JavaScript headers result type: {}, value: {}", jsHeaderResult.getClass().getName(), jsHeaderResult);

                    if (jsHeaderResult instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> jsHeaderMap = (java.util.Map<String, Object>) jsHeaderResult;
                        headers.clear();
                        for (Map.Entry<String, Object> entry : jsHeaderMap.entrySet()) {
                            if (entry.getValue() != null) {
                                headers.put(entry.getKey(), entry.getValue().toString());
                            }
                        }
                        log.info("Synced headers from JavaScript: {}", headers);
                    } else {
                        // Try to access as object properties using eval
                        String jsHeaderKeys = (String) scriptEngine.eval("Object.keys(__jsHeaders).join(',')", bindings);
                        log.info("JavaScript header keys: {}", jsHeaderKeys);

                        if (jsHeaderKeys != null && !jsHeaderKeys.isEmpty()) {
                            headers.clear();
                            for (String key : jsHeaderKeys.split(",")) {
                                if (!key.trim().isEmpty()) {
                                    Object value = scriptEngine.eval("__jsHeaders['" + key.trim() + "']", bindings);
                                    if (value != null) {
                                        headers.put(key.trim(), value.toString());
                                        log.info("Synced header: {} = {}", key.trim(), value);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error syncing JavaScript objects back to Java: {}", e.getMessage(), e);
                }

                // Debug: Check headers after script execution
                log.info("Headers after script execution: {}", headers);
                log.info("Variables after script execution: {}", variables);

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
