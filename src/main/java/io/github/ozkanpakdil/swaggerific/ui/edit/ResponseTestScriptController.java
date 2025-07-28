package io.github.ozkanpakdil.swaggerific.ui.edit;

import io.github.ozkanpakdil.swaggerific.data.EnvironmentManager;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.ui.textfx.JavaScriptColorize;
import io.swagger.v3.core.util.Json;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Controller for the response test script editor.
 * This controller handles the execution of JavaScript test scripts for validating API responses.
 */
public class ResponseTestScriptController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(ResponseTestScriptController.class);

    @FXML
    private CodeArea codeResponseTestScript;

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private ScriptEngine scriptEngine;
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private final JavaScriptColorize javaScriptColorize = new JavaScriptColorize();
    private Runnable onScriptExecutionComplete;
    private EnvironmentManager environmentManager;
    private final List<AssertionResult> assertionResults = new ArrayList<>();

    /**
     * Initializes the JavaScript engine for executing test scripts.
     * Tries multiple engine names that GraalVM might use, falls back to other JavaScript engines if needed.
     *
     * @return the initialized script engine
     */
    private ScriptEngine initializeScriptEngine() {
        // Try different engine names that GraalVM might use
        String[] engineNames = { "graal.js", "js", "JavaScript", "javascript", "ECMAScript", "ecmascript" };

        for (String engineName : engineNames) {
            ScriptEngine engine = scriptEngineManager.getEngineByName(engineName);
            if (engine != null) {
                log.info("Successfully initialized JavaScript engine: {}", engineName);
                return engine;
            }
        }

        log.error("No JavaScript engine found! Available engines:");
        scriptEngineManager.getEngineFactories().forEach(factory -> log.error(
                "  Engine: {} ({}), Language: {} ({}), Extensions: {}",
                factory.getEngineName(), factory.getEngineVersion(),
                factory.getLanguageName(), factory.getLanguageVersion(),
                factory.getExtensions()));

        return null;
    }

    /**
     * Initializes the controller.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize the script engine if not already initialized
        if (scriptEngine == null) {
            scriptEngine = initializeScriptEngine();
            log.info("Script engine initialized in initialize(): {}", 
                    scriptEngine != null ? scriptEngine.getClass().getName() : "null");
        }
        setupCodeEditor();
    }

    /**
     * Sets up the code editor with syntax highlighting and line numbers.
     */
    private void setupCodeEditor() {
        codeResponseTestScript.setParagraphGraphicFactory(LineNumberFactory.get(codeResponseTestScript));
        
        // Apply syntax highlighting CSS
        URL cssUrl = getClass().getResource("/css/javascript-highlighting.css");
        if (cssUrl != null) {
            codeResponseTestScript.getStylesheets().add(cssUrl.toString());
        }
        
        // Enable word wrapping and line highlighting
        codeResponseTestScript.setWrapText(true);
        codeResponseTestScript.setLineHighlighterOn(true);
        
        // Add listener for syntax highlighting
        codeResponseTestScript.textProperty().addListener(
                (obs, oldText, newText) -> refreshSyntaxHighlighting());
    }

    /**
     * Sets a callback to be executed when script execution is complete.
     *
     * @param callback the callback to execute
     */
    public void setOnScriptExecutionComplete(Runnable callback) {
        this.onScriptExecutionComplete = callback;
    }

    /**
     * Validates that the script engine is properly initialized.
     *
     * @throws RuntimeException if script engine is not available
     */
    private void validateScriptEngine() {
        if (scriptEngine == null) {
            scriptEngine = initializeScriptEngine();
            if (scriptEngine == null) {
                String errorMsg = "JavaScript engine is not available. Please ensure GraalVM JavaScript engine is properly configured.";
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        }
    }

    /**
     * Creates bindings for the script execution context.
     * This makes variables and functions available to the script.
     *
     * @param response the HTTP response to test
     * @return the script bindings
     */
    private SimpleBindings createScriptBindings(HttpResponse response) {
        SimpleBindings bindings = new SimpleBindings();
        
        // Add console object for logging
        Map<String, Object> console = new HashMap<>();
        List<Object> consoleLogs = new ArrayList<>();
        console.put("logs", consoleLogs);
        
        // Console logging methods
        console.put("log", (Function<Object, Void>) message -> {
            consoleLogs.add(message);
            return null;
        });
        
        console.put("info", (Function<Object, Void>) message -> {
            consoleLogs.add("INFO: " + message);
            return null;
        });
        
        console.put("warn", (Function<Object, Void>) message -> {
            consoleLogs.add("WARNING: " + message);
            return null;
        });
        
        console.put("error", (Function<Object, Void>) message -> {
            consoleLogs.add("ERROR: " + message);
            return null;
        });
        
        console.put("debug", (Function<Object, Void>) message -> {
            consoleLogs.add("DEBUG: " + message);
            return null;
        });
        
        bindings.put("console", console);
        
        // Add pm object for Postman-like API
        Map<String, Object> pm = new HashMap<>();
        
        // Add response object
        Map<String, Object> responseObj = new HashMap<>();
        responseObj.put("status", response.statusCode());
        responseObj.put("body", response.body());
        responseObj.put("headers", response.headers());
        responseObj.put("contentType", response.contentType());
        
        // Add JSON parsing utility
        responseObj.put("json", (Function<Void, Object>) v -> {
            try {
                return Json.mapper().readValue(response.body(), Object.class);
            } catch (Exception e) {
                log.warn("Failed to parse response body as JSON: {}", e.getMessage());
                return null;
            }
        });
        
        pm.put("response", responseObj);
        
        // Add test object for assertions
        Map<String, Object> test = new HashMap<>();
        
        // Add assertion methods
        test.put("assertEquals", (Function<Object[], Boolean>) args -> {
            if (args.length < 3) {
                String message = args.length > 0 ? args[0].toString() : "Assertion failed";
                addAssertionResult(false, message + ": Not enough arguments for assertEquals");
                return false;
            }
            
            String message = args[0].toString();
            Object actual = args[1];
            Object expected = args[2];
            
            boolean result = (actual == null && expected == null) || 
                             (actual != null && actual.equals(expected));
            
            addAssertionResult(result, message);
            return result;
        });
        
        test.put("assertTrue", (Function<Object[], Boolean>) args -> {
            if (args.length < 2) {
                String message = args.length > 0 ? args[0].toString() : "Assertion failed";
                addAssertionResult(false, message + ": Not enough arguments for assertTrue");
                return false;
            }
            
            String message = args[0].toString();
            Object condition = args[1];
            
            boolean result = condition instanceof Boolean && (Boolean) condition;
            
            addAssertionResult(result, message);
            return result;
        });
        
        test.put("assertFalse", (Function<Object[], Boolean>) args -> {
            if (args.length < 2) {
                String message = args.length > 0 ? args[0].toString() : "Assertion failed";
                addAssertionResult(false, message + ": Not enough arguments for assertFalse");
                return false;
            }
            
            String message = args[0].toString();
            Object condition = args[1];
            
            boolean result = condition instanceof Boolean && !(Boolean) condition;
            
            addAssertionResult(result, message);
            return result;
        });
        
        test.put("assertContains", (Function<Object[], Boolean>) args -> {
            if (args.length < 3) {
                String message = args.length > 0 ? args[0].toString() : "Assertion failed";
                addAssertionResult(false, message + ": Not enough arguments for assertContains");
                return false;
            }
            
            String message = args[0].toString();
            String haystack = args[1] != null ? args[1].toString() : "";
            String needle = args[2] != null ? args[2].toString() : "";
            
            boolean result = haystack.contains(needle);
            
            addAssertionResult(result, message);
            return result;
        });
        
        test.put("assertStatusCode", (Function<Object[], Boolean>) args -> {
            if (args.length < 2) {
                String message = "Status code assertion failed: Not enough arguments";
                addAssertionResult(false, message);
                return false;
            }
            
            String message = args.length > 2 ? args[0].toString() : 
                             "Status code should be " + args[args.length - 1];
            int expectedStatus = args[args.length - 1] instanceof Number ? 
                                ((Number) args[args.length - 1]).intValue() : -1;
            
            boolean result = response.statusCode() == expectedStatus;
            
            addAssertionResult(result, message);
            return result;
        });
        
        test.put("assertHeader", (Function<Object[], Boolean>) args -> {
            if (args.length < 2) {
                String message = "Header assertion failed: Not enough arguments";
                addAssertionResult(false, message);
                return false;
            }
            
            String headerName = args[args.length - 1].toString();
            String message = args.length > 2 ? args[0].toString() : 
                             "Response should have header '" + headerName + "'";
            
            boolean result = response.headers().containsKey(headerName);
            
            addAssertionResult(result, message);
            return result;
        });
        
        test.put("assertHeaderValue", (Function<Object[], Boolean>) args -> {
            if (args.length < 3) {
                String message = "Header value assertion failed: Not enough arguments";
                addAssertionResult(false, message);
                return false;
            }
            
            String headerName = args[args.length - 2].toString();
            String expectedValue = args[args.length - 1].toString();
            String message = args.length > 3 ? args[0].toString() : 
                             "Header '" + headerName + "' should have value '" + expectedValue + "'";
            
            String actualValue = response.headers().get(headerName);
            boolean result = actualValue != null && actualValue.equals(expectedValue);
            
            addAssertionResult(result, message);
            return result;
        });
        
        pm.put("test", test);
        
        // Add environment variables
        Map<String, Object> environment = new HashMap<>();
        Map<String, Object> variables = new HashMap<>();
        
        // Add variable getter and setter
        variables.put("get", (Function<String, Object>) name -> {
            if (this.variables.containsKey(name)) {
                return this.variables.get(name);
            } else if (environmentManager != null) {
                return environmentManager.getVariableValue(name);
            }
            return null;
        });
        
        variables.put("set", (Function<Object[], Void>) args -> {
            if (args.length >= 2) {
                String name = args[0].toString();
                Object value = args[1];
                this.variables.put(name, value);
            }
            return null;
        });
        
        pm.put("environment", environment);
        pm.put("variables", variables);
        
        bindings.put("pm", pm);
        
        return bindings;
    }

    /**
     * Adds an assertion result to the list of results.
     *
     * @param passed whether the assertion passed
     * @param message the assertion message
     */
    private void addAssertionResult(boolean passed, String message) {
        assertionResults.add(new AssertionResult(passed, message));
        log.info("Assertion {}: {}", passed ? "PASSED" : "FAILED", message);
    }

    /**
     * Verifies that the pm object is properly initialized in the script bindings.
     *
     * @param bindings the script bindings
     */
    private void verifyPmObject(SimpleBindings bindings) {
        if (!bindings.containsKey("pm")) {
            throw new RuntimeException("pm object not found in script bindings");
        }
        
        Object pm = bindings.get("pm");
        if (!(pm instanceof Map)) {
            throw new RuntimeException("pm object is not a Map");
        }
    }

    /**
     * Executes a script with the given bindings.
     *
     * @param script the script to execute
     * @param bindings the script bindings
     */
    private void executeScriptWithBindings(String script, SimpleBindings bindings) {
        try {
            // Clear previous assertion results
            assertionResults.clear();
            
            // Execute the script
            scriptEngine.eval(script, bindings);
            
            // Process console logs
            processConsoleLogs(bindings);
            
            // Log assertion results
            int passedCount = 0;
            int failedCount = 0;
            
            for (AssertionResult result : assertionResults) {
                if (result.passed()) {
                    passedCount++;
                } else {
                    failedCount++;
                }
            }
            
            log.info("Test results: {} passed, {} failed", passedCount, failedCount);
            
            // If there are failed assertions, log them
            if (failedCount > 0) {
                log.warn("Failed assertions:");
                for (AssertionResult result : assertionResults) {
                    if (!result.passed()) {
                        log.warn("  - {}", result.message());
                    }
                }
            }
            
        } catch (ScriptException e) {
            throw handleScriptError(e, "Error executing response test script");
        }
    }

    /**
     * Processes console logs from the script execution.
     *
     * @param bindings the script bindings
     */
    private void processConsoleLogs(SimpleBindings bindings) {
        try {
            Object console = bindings.get("console");
            if (console instanceof Map) {
                Map<?, ?> consoleMap = (Map<?, ?>) console;
                Object logs = consoleMap.get("logs");
                
                if (logs instanceof List) {
                    processConsoleLogsList((List<Object>) logs);
                }
            }
        } catch (Exception e) {
            log.warn("Error processing console logs: {}", e.getMessage());
        }
    }

    /**
     * Processes a list of console logs.
     *
     * @param consoleLogs the list of console logs
     */
    private void processConsoleLogsList(List<Object> consoleLogs) {
        for (Object logMessage : consoleLogs) {
            logConsoleMessage(logMessage);
        }
    }

    /**
     * Logs a console message.
     *
     * @param logMessage the message to log
     */
    private void logConsoleMessage(Object logMessage) {
        if (logMessage == null) {
            log.info("[Console] null");
        } else if (logMessage instanceof Map || logMessage instanceof List) {
            try {
                log.info("[Console] {}", Json.mapper().writeValueAsString(logMessage));
            } catch (Exception e) {
                log.info("[Console] {}", logMessage);
            }
        } else {
            log.info("[Console] {}", logMessage);
        }
    }

    /**
     * Notifies that script execution is complete.
     */
    private void notifyScriptComplete() {
        if (onScriptExecutionComplete != null) {
            try {
                onScriptExecutionComplete.run();
            } catch (Exception e) {
                log.error("Error in script completion callback: {}", e.getMessage());
            }
        }
    }

    /**
     * Handles a script error.
     *
     * @param e the exception
     * @param context the error context
     * @return a runtime exception with the error details
     */
    private RuntimeException handleScriptError(Exception e, String context) {
        log.error("{}: {}", context, e.getMessage());
        return new RuntimeException(context + ": " + e.getMessage(), e);
    }

    /**
     * Executes the response test script.
     *
     * @param response the HTTP response to test
     * @return a future that completes when the script execution is complete
     */
    public CompletableFuture<List<AssertionResult>> executeScript(HttpResponse response) {
        CompletableFuture<List<AssertionResult>> future = new CompletableFuture<>();
        
        try {
            validateScriptEngine();
            
            String script = getScript();
            if (script == null || script.trim().isEmpty()) {
                log.info("No response test script to execute");
                future.complete(assertionResults);
                return future;
            }
            
            // Create bindings with the response
            SimpleBindings bindings = createScriptBindings(response);
            
            // Execute the script
            executeScriptWithBindings(script, bindings);
            
            // Complete the future with the assertion results
            future.complete(new ArrayList<>(assertionResults));
            
        } catch (Exception e) {
            log.error("Error executing response test script: {}", e.getMessage());
            showScriptError(e);
            future.completeExceptionally(e);
        } finally {
            notifyScriptComplete();
        }
        
        return future;
    }

    /**
     * Shows a script error dialog.
     *
     * @param e the exception
     */
    private void showScriptError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, 
                "Error executing response test script: " + e.getMessage(), 
                ButtonType.OK);
        alert.setTitle("Script Error");
        alert.setHeaderText("Response Test Script Error");
        alert.showAndWait();
    }

    /**
     * Gets the current script text.
     *
     * @return the script text
     */
    public String getScript() {
        return codeResponseTestScript.getText();
    }

    /**
     * Sets the script text.
     *
     * @param script the script text
     */
    public void setScript(String script) {
        codeResponseTestScript.replaceText(script);
    }

    /**
     * Gets the variables map.
     *
     * @return the variables map
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * Sets the environment manager.
     *
     * @param environmentManager the environment manager
     */
    public void setEnvironmentManager(EnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
        log.info("Environment manager set in response test script controller");
    }

    /**
     * Resolves environment variables in the given input.
     *
     * @param input the input string
     * @return the input with environment variables resolved
     */
    public String resolveEnvironmentVariables(String input) {
        if (input == null || input.isEmpty() || environmentManager == null) {
            return input;
        }
        
        return environmentManager.resolveVariables(input);
    }

    /**
     * Refreshes the syntax highlighting in the code editor.
     */
    private void refreshSyntaxHighlighting() {
        String text = codeResponseTestScript.getText();
        codeResponseTestScript.setStyleSpans(0, javaScriptColorize.computeHighlighting(text));
    }

    /**
     * Inserts a snippet for asserting status code.
     */
    @FXML
    public void insertAssertStatusCodeSnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Assert that the status code is 200\n" +
                         "pm.test.assertStatusCode(\"Status code should be 200\", 200);\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a snippet for asserting status code range.
     */
    @FXML
    public void insertAssertStatusCodeRangeSnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Assert that the status code is in the 2xx range\n" +
                         "pm.test.assertTrue(\"Status code should be in 2xx range\", \n" +
                         "    pm.response.status >= 200 && pm.response.status < 300);\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a snippet for asserting header exists.
     */
    @FXML
    public void insertAssertHeaderExistsSnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Assert that a specific header exists\n" +
                         "pm.test.assertHeader(\"Response should have Content-Type header\", \"Content-Type\");\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a snippet for asserting header value.
     */
    @FXML
    public void insertAssertHeaderValueSnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Assert that a header has a specific value\n" +
                         "pm.test.assertHeaderValue(\"Content-Type should be application/json\", \n" +
                         "    \"Content-Type\", \"application/json\");\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a snippet for asserting JSON property.
     */
    @FXML
    public void insertAssertJsonPropertySnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Assert that a JSON property has a specific value\n" +
                         "const jsonData = pm.response.json();\n" +
                         "pm.test.assertEquals(\"Property should have expected value\", \n" +
                         "    jsonData.propertyName, \"expectedValue\");\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a snippet for asserting JSON array length.
     */
    @FXML
    public void insertAssertJsonArrayLengthSnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Assert that a JSON array has a specific length\n" +
                         "const jsonData = pm.response.json();\n" +
                         "pm.test.assertEquals(\"Array should have expected length\", \n" +
                         "    jsonData.arrayProperty.length, 3);\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a snippet for asserting body contains.
     */
    @FXML
    public void insertAssertBodyContainsSnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Assert that the response body contains a specific string\n" +
                         "pm.test.assertContains(\"Response should contain expected text\", \n" +
                         "    pm.response.body, \"expected text\");\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a snippet for getting a variable.
     */
    @FXML
    public void insertGetVariableSnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Get a variable\n" +
                         "const myVar = pm.variables.get(\"variableName\");\n" +
                         "console.log(\"Variable value:\", myVar);\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a snippet for setting a variable.
     */
    @FXML
    public void insertSetVariableSnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Set a variable\n" +
                         "pm.variables.set(\"variableName\", \"variableValue\");\n" +
                         "console.log(\"Variable set\");\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a snippet for logging.
     */
    @FXML
    public void insertLogSnippet() {
        int position = codeResponseTestScript.getCaretPosition();
        String snippet = "// Log a message\n" +
                         "console.log(\"Log message\");\n";
        codeResponseTestScript.insertText(position, snippet);
    }

    /**
     * Inserts a complete example snippet.
     */
    @FXML
    public void insertCompleteExampleSnippet() {
        String snippet = "// Complete response test example\n\n" +
                         "// Test status code\n" +
                         "pm.test.assertStatusCode(\"Status code should be 200\", 200);\n\n" +
                         "// Test headers\n" +
                         "pm.test.assertHeader(\"Response should have Content-Type header\", \"Content-Type\");\n" +
                         "pm.test.assertHeaderValue(\"Content-Type should be application/json\", \n" +
                         "    \"Content-Type\", \"application/json\");\n\n" +
                         "// Parse JSON response\n" +
                         "const jsonData = pm.response.json();\n\n" +
                         "// Test JSON properties\n" +
                         "pm.test.assertTrue(\"Response should have id property\", \n" +
                         "    jsonData.hasOwnProperty(\"id\"));\n\n" +
                         "// Store a value from the response in a variable\n" +
                         "if (jsonData.id) {\n" +
                         "    pm.variables.set(\"lastId\", jsonData.id);\n" +
                         "    console.log(\"Saved ID:\", jsonData.id);\n" +
                         "}\n\n" +
                         "// Log the response\n" +
                         "console.log(\"Response received:\", jsonData);\n";
        
        codeResponseTestScript.replaceText(snippet);
    }

    /**
     * Represents the result of an assertion.
     */
    public record AssertionResult(boolean passed, String message) {
    }
}