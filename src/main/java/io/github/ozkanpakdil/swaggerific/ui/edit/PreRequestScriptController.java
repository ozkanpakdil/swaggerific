package io.github.ozkanpakdil.swaggerific.ui.edit;

import io.github.ozkanpakdil.swaggerific.data.EnvironmentManager;
import io.github.ozkanpakdil.swaggerific.tools.HttpUtility;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpService;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpServiceImpl;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
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
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Controller for the Pre-request Script tab in the request panel. Handles JavaScript execution before sending the main HTTP
 * request.
 */
public class PreRequestScriptController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(PreRequestScriptController.class);

    @FXML
    private CodeArea codePreRequestScript;

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private final ScriptEngine scriptEngine = initializeScriptEngine();

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

        try {
            log.error("No JavaScript engine found! Available engines:");
            scriptEngineManager.getEngineFactories().forEach(factory -> log.error(
                    "  Engine: {} ({}), Language: {} ({}), Extensions: {}",
                    factory.getEngineName(), factory.getEngineVersion(),
                    factory.getLanguageName(), factory.getLanguageVersion(),
                    factory.getExtensions()));
        } catch (Exception e) {
            log.error("Error initializing JavaScript engine: {}", e.getMessage(), e);
        }
        return null;
    }

    private final HttpService httpService = new HttpServiceImpl();
    private final HttpUtility httpUtility = new HttpUtility();

    // For syntax highlighting
    private final JavaScriptColorize javaScriptColorize = new JavaScriptColorize();

    // Store for variables that can be accessed across script executions
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    
    // Environment manager for accessing environment variables
    private EnvironmentManager environmentManager;

    // Callback to be called when script execution is complete
    private Runnable onScriptExecutionComplete;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up code editor with JavaScript syntax highlighting
        setupCodeEditor();
    }

    /**
     * Sets up the code editor with JavaScript syntax highlighting
     */
    private void setupCodeEditor() {
        // Add line numbers
        codePreRequestScript.setParagraphGraphicFactory(LineNumberFactory.get(codePreRequestScript));

        URL cssUrl = MainController.class.getResource("/css/javascript-highlighting.css");
        if (cssUrl != null) {
            // Add CSS for syntax highlighting
            codePreRequestScript.getStylesheets().add(cssUrl.toString());
        } else {
            log.warn("Could not find JavaScript highlighting CSS file");
        }

        // Enable text wrapping and line highlighting
        codePreRequestScript.setWrapText(true);
        codePreRequestScript.setLineHighlighterOn(true);

        // Apply JavaScript syntax highlighting
        codePreRequestScript.textProperty().addListener(
                (obs, oldText, newText) -> refreshSyntaxHighlighting());
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
     * Validates that the script engine is available
     *
     * @throws RuntimeException if script engine is not available
     */
    private void validateScriptEngine() {
        if (scriptEngine == null) {
            String errorMsg = "JavaScript engine is not available. Please ensure GraalVM JavaScript engine is properly configured.";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * Creates script bindings with variables and headers
     *
     * @param headers The headers map to include in bindings
     * @return The bindings object with initialized JavaScript environment
     */
    @SuppressWarnings("null") // scriptEngine is validated before this method is called
    private SimpleBindings createScriptBindings(Map<String, String> headers) throws Exception {
        SimpleBindings bindings = new SimpleBindings();

        // Convert Java variables to JavaScript object
        String jsVariables = Json.mapper().writeValueAsString(variables);

        // Convert Java headers to JavaScript object
        String jsHeaders = Json.mapper().writeValueAsString(headers);
        
        // Get active environment name and variables
        String activeEnvironmentName = "";
        String jsEnvironmentVariables = "{}";
        if (environmentManager != null) {
            environmentManager.getActiveEnvironment().ifPresent(env -> {
                log.info("Active environment: {}", env.getName());
            });
            
            // Convert environment variables to JSON
            try {
                jsEnvironmentVariables = Json.mapper().writeValueAsString(
                    environmentManager.getActiveEnvironment()
                        .map(env -> env.getAllVariables().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                var -> var.getKey(),
                                var -> var.getValue())))
                        .orElse(new java.util.HashMap<>())
                );
                
                activeEnvironmentName = environmentManager.getActiveEnvironment()
                    .map(env -> env.getName())
                    .orElse("");
            } catch (Exception e) {
                log.error("Error converting environment variables to JSON", e);
            }
        }

        // Debug info
        log.info("Bindings keys: {}", bindings.keySet());
        log.info("Headers object: {}", headers);

        // Create pm object structure with enhanced features
        String pmSetupScript = String.format("""
                // Setup error handling with line numbers and stack traces
                var __lastError = null;
                var __promiseQueue = [];
                var __jsVariables = %s;
                var __jsHeaders = %s;
                var __jsEnvironmentVariables = %s;
                var __activeEnvironmentName = "%s";
                var __consoleLogs = [];
                
                // Enhanced console with more methods
                var console = {
                  log: function(message) { __consoleLogs.push('[CONSOLE.LOG] ' + message); },
                  error: function(message) { __consoleLogs.push('[CONSOLE.ERROR] ' + message); },
                  warn: function(message) { __consoleLogs.push('[CONSOLE.WARN] ' + message); },
                  info: function(message) { __consoleLogs.push('[CONSOLE.INFO] ' + message); },
                  debug: function(message) { __consoleLogs.push('[CONSOLE.DEBUG] ' + message); },
                  trace: function(message) { 
                    try { throw new Error('Trace'); } 
                    catch(e) { 
                      __consoleLogs.push('[CONSOLE.TRACE] ' + message + '\\n' + e.stack); 
                    }
                  },
                  assert: function(condition, message) {
                    if (!condition) {
                      __consoleLogs.push('[CONSOLE.ASSERT] Assertion failed: ' + (message || 'Assertion failed'));
                    }
                  },
                  table: function(data) {
                    if (Array.isArray(data) || typeof data === 'object') {
                      __consoleLogs.push('[CONSOLE.TABLE] ' + JSON.stringify(data, null, 2));
                    } else {
                      __consoleLogs.push('[CONSOLE.TABLE] Invalid data for table: ' + data);
                    }
                  }
                };
                
                // Enhanced pm object with more methods and properties
                var pm = {
                  // Enhanced variables API
                  variables: {
                    get: function(key) {
                      var value = __jsVariables[key];
                      if (value === undefined) {
                        console.warn('Variable "' + key + '" is undefined. Available variables: ' + Object.keys(__jsVariables).join(', '));
                      } else {
                        console.log('Retrieved variable "' + key + '" = ' + value);
                      }
                      return value;
                    },
                    set: function(key, value) {
                      console.log('Setting variable "' + key + '" = ' + value);
                      __jsVariables[key] = value;
                    },
                    has: function(key) {
                      return __jsVariables.hasOwnProperty(key);
                    },
                    unset: function(key) {
                      if (__jsVariables.hasOwnProperty(key)) {
                        console.log('Removing variable "' + key + '"');
                        delete __jsVariables[key];
                        return true;
                      }
                      return false;
                    },
                    clear: function() {
                      console.log('Clearing all variables');
                      Object.keys(__jsVariables).forEach(function(key) {
                        delete __jsVariables[key];
                      });
                    },
                    toObject: function() {
                      return Object.assign({}, __jsVariables);
                    }
                  },
                  
                  // Enhanced environment API
                  environment: {
                    name: __activeEnvironmentName,
                    get: function(key) {
                      var value = __jsEnvironmentVariables[key];
                      if (value === undefined) {
                        console.warn('Environment variable "' + key + '" is undefined. Available variables: ' + Object.keys(__jsEnvironmentVariables).join(', '));
                      } else {
                        console.log('Retrieved environment variable "' + key + '" = ' + value);
                      }
                      return value;
                    },
                    has: function(key) {
                      return __jsEnvironmentVariables.hasOwnProperty(key);
                    },
                    toObject: function() {
                      return Object.assign({}, __jsEnvironmentVariables);
                    }
                  },
                  
                  // Enhanced request API
                  request: {
                    headers: __jsHeaders,
                    addHeader: function(name, value) {
                      this.headers[name] = value;
                      console.log('Added header "' + name + '" = "' + value + '"');
                    },
                    removeHeader: function(name) {
                      if (this.headers.hasOwnProperty(name)) {
                        delete this.headers[name];
                        console.log('Removed header "' + name + '"');
                        return true;
                      }
                      return false;
                    },
                    getHeader: function(name) {
                      return this.headers[name];
                    },
                    hasHeader: function(name) {
                      return this.headers.hasOwnProperty(name);
                    }
                  },
                  
                  // Enhanced sendRequest with Promise support
                  sendRequest: function(url, options, callback) {
                    console.log('sendRequest called with URL: ' + url);
                    
                    // Handle different parameter combinations
                    if (typeof options === 'function') {
                      callback = options;
                      options = {};
                    }
                    
                    options = options || {};
                    
                    // Create a promise that will be resolved/rejected by the Java code
                    var promise = new Promise(function(resolve, reject) {
                      // Store the promise callbacks in the queue for later execution
                      __promiseQueue.push({
                        url: url,
                        options: options,
                        resolve: resolve,
                        reject: reject
                      });
                      
                      console.log('Added request to promise queue: ' + url);
                    });
                    
                    // If a callback was provided, attach it to the promise
                    if (typeof callback === 'function') {
                      promise.then(
                        function(response) { callback(null, response); },
                        function(error) { callback(error, null); }
                      );
                    }
                    
                    return promise;
                  },
                  
                  // Utility methods
                  utils: {
                    // JSON utilities
                    json: {
                      parse: function(text) {
                        try {
                          return JSON.parse(text);
                        } catch (e) {
                          console.error('JSON parse error: ' + e.message);
                          return null;
                        }
                      },
                      stringify: function(obj, replacer, space) {
                        try {
                          return JSON.stringify(obj, replacer, space);
                        } catch (e) {
                          console.error('JSON stringify error: ' + e.message);
                          return '';
                        }
                      }
                    },
                    
                    // String utilities
                    string: {
                      isEmpty: function(str) {
                        return !str || str.trim().length === 0;
                      },
                      isBlank: function(str) {
                        return !str || /^\\s*$/.test(str);
                      },
                      trim: function(str) {
                        return str ? str.trim() : '';
                      }
                    },
                    
                    // Base64 encoding/decoding
                    base64: {
                      encode: function(text) {
                        try {
                          // Use browser's btoa if available, otherwise fallback
                          if (typeof btoa === 'function') {
                            return btoa(text);
                          }
                          // Simple fallback implementation
                          var keyStr = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
                          var output = '';
                          var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
                          var i = 0;
                          
                          while (i < text.length) {
                            chr1 = text.charCodeAt(i++);
                            chr2 = i < text.length ? text.charCodeAt(i++) : NaN;
                            chr3 = i < text.length ? text.charCodeAt(i++) : NaN;
                            
                            enc1 = chr1 >> 2;
                            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
                            enc3 = isNaN(chr2) ? 64 : ((chr2 & 15) << 2) | (chr3 >> 6);
                            enc4 = isNaN(chr3) ? 64 : (chr3 & 63);
                            
                            output += keyStr.charAt(enc1) + keyStr.charAt(enc2) + keyStr.charAt(enc3) + keyStr.charAt(enc4);
                          }
                          return output;
                        } catch (e) {
                          console.error('Base64 encode error: ' + e.message);
                          return '';
                        }
                      },
                      decode: function(text) {
                        try {
                          // Use browser's atob if available, otherwise fallback
                          if (typeof atob === 'function') {
                            return atob(text);
                          }
                          // Simple fallback implementation
                          var keyStr = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
                          var output = '';
                          var chr1, chr2, chr3;
                          var enc1, enc2, enc3, enc4;
                          var i = 0;
                          
                          // Remove all characters that are not A-Z, a-z, 0-9, +, /, or =
                          text = text.replace(/[^A-Za-z0-9\\+\\/\\=]/g, '');
                          
                          while (i < text.length) {
                            enc1 = keyStr.indexOf(text.charAt(i++));
                            enc2 = keyStr.indexOf(text.charAt(i++));
                            enc3 = keyStr.indexOf(text.charAt(i++));
                            enc4 = keyStr.indexOf(text.charAt(i++));
                            
                            chr1 = (enc1 << 2) | (enc2 >> 4);
                            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
                            chr3 = ((enc3 & 3) << 6) | enc4;
                            
                            output += String.fromCharCode(chr1);
                            
                            if (enc3 !== 64) {
                              output += String.fromCharCode(chr2);
                            }
                            if (enc4 !== 64) {
                              output += String.fromCharCode(chr3);
                            }
                          }
                          return output;
                        } catch (e) {
                          console.error('Base64 decode error: ' + e.message);
                          return '';
                        }
                      }
                    }
                  },
                  
                  // Information about the current execution environment
                  info: {
                    version: '1.0.0',
                    engine: 'GraalVM JavaScript',
                    timestamp: Date.now()
                  }
                };
                """, jsVariables, jsHeaders, jsEnvironmentVariables, activeEnvironmentName);

        try {
            log.debug("Executing pm setup script: {}", pmSetupScript);
            scriptEngine.eval(pmSetupScript, bindings);
            log.debug("PM setup script executed successfully");

            verifyPmObject(bindings);
        } catch (Exception e) {
            log.error("Error executing pm setup script: {}", e.getMessage(), e);
            throw e;
        }

        return bindings;
    }

    private void verifyPmObject(SimpleBindings bindings) throws ScriptException {
        Object pmTest = scriptEngine.eval("typeof pm", bindings);
        log.info("PM object type: {}", pmTest);

        if ("object".equals(pmTest)) {
            Object pmVarTest = scriptEngine.eval("typeof pm.variables", bindings);
            log.info("PM variables type: {}", pmVarTest);
        }
    }

    /**
     * Executes the script with given bindings and processes console logs
     * Also handles errors with line numbers and stack traces
     */
    @SuppressWarnings("null") // scriptEngine is validated before this method is called
    private void executeScriptWithBindings(String script, SimpleBindings bindings) throws ScriptException {
        try {
            // Wrap the script in a try-catch block to capture line numbers and stack traces
            String wrappedScript = """
                    try {
                        %s
                    } catch (e) {
                        __lastError = {
                            message: e.message,
                            stack: e.stack,
                            lineNumber: e.lineNumber,
                            columnNumber: e.columnNumber,
                            name: e.name
                        };
                        console.error('Script error: ' + e.message + ' at line ' + e.lineNumber);
                        console.error('Stack trace: ' + e.stack);
                        throw e;
                    }
                    """.formatted(script);
            
            scriptEngine.eval(wrappedScript, bindings);
            
            // Process any promises in the queue
            processPromiseQueue(bindings);
            
            // Process console logs
            processConsoleLogs(bindings);
            
            // Check if there was an error
            Object lastError = scriptEngine.eval("__lastError", bindings);
            if (lastError != null && !"null".equals(lastError.toString())) {
                log.error("Script execution error: {}", lastError);
            }
        } catch (ScriptException e) {
            // Extract line number and column information if available
            int lineNumber = e.getLineNumber();
            int columnNumber = e.getColumnNumber();
            
            if (lineNumber > 0) {
                // Adjust line number to account for the wrapper
                lineNumber = Math.max(1, lineNumber - 1);
                
                // Get the line of code that caused the error
                String[] lines = script.split("\n");
                String errorLine = lineNumber <= lines.length ? lines[lineNumber - 1] : "unknown";
                
                log.error("Script error at line {}, column {}: {}", lineNumber, columnNumber, e.getMessage());
                log.error("Error line: {}", errorLine);
            } else {
                log.error("Script error: {}", e.getMessage());
            }
            
            throw e;
        }
    }
    
    /**
     * Processes any promises in the queue
     */
    private void processPromiseQueue(SimpleBindings bindings) {
        try {
            // Check if there are any promises in the queue
            Object queueResult = scriptEngine.eval("__promiseQueue", bindings);
            if (queueResult instanceof java.util.List<?>) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> promiseQueue = (java.util.List<Object>) queueResult;
                
                if (!promiseQueue.isEmpty()) {
                    log.info("Processing {} promises in queue", promiseQueue.size());
                    
                    // Process each promise in the queue
                    for (Object promiseObj : promiseQueue) {
                        if (promiseObj instanceof java.util.Map<?, ?>) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> promise = (java.util.Map<String, Object>) promiseObj;
                            
                            String url = promise.get("url").toString();
                            log.info("Processing promise for URL: {}", url);
                            
                            try {
                                // Here we would actually make the HTTP request
                                // For now, we'll just simulate a successful response
                                String responseJson = """
                                        {
                                            "status": 200,
                                            "statusText": "OK",
                                            "headers": {"Content-Type": "application/json"},
                                            "body": "{\\"success\\": true, \\"message\\": \\"Request successful\\"}",
                                            "json": function() { return {"success": true, "message": "Request successful"}; }
                                        }
                                        """;
                                
                                // Resolve the promise
                                String resolveScript = String.format(
                                        "var response = %s; __promiseQueue[0].resolve(response);", 
                                        responseJson);
                                scriptEngine.eval(resolveScript, bindings);
                                
                                log.info("Resolved promise for URL: {}", url);
                            } catch (Exception e) {
                                // Reject the promise
                                String rejectScript = String.format(
                                        "var error = {message: '%s'}; __promiseQueue[0].reject(error);", 
                                        e.getMessage().replace("'", "\\'"));
                                scriptEngine.eval(rejectScript, bindings);
                                
                                log.error("Rejected promise for URL: {}: {}", url, e.getMessage());
                            }
                        }
                    }
                    
                    // Clear the promise queue
                    scriptEngine.eval("__promiseQueue = [];", bindings);
                }
            }
        } catch (Exception e) {
            log.warn("Error processing promise queue: {}", e.getMessage());
        }
    }

    /**
     * Processes console logs from script execution
     */
    private void processConsoleLogs(SimpleBindings bindings) {
        try {
            Object consoleLogsResult = scriptEngine.eval("__consoleLogs", bindings);
            if (consoleLogsResult instanceof java.util.List<?>) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> consoleLogs = (java.util.List<Object>) consoleLogsResult;
                processConsoleLogsList(consoleLogs);
            } else {
                processConsoleLogsArray(bindings);
            }
        } catch (Exception e) {
            log.warn("Error reading console logs: {}", e.getMessage());
        }
    }

    private void processConsoleLogsList(java.util.List<Object> consoleLogs) {
        for (Object logMessage : consoleLogs) {
            logConsoleMessage(logMessage);
        }
    }

    private void processConsoleLogsArray(SimpleBindings bindings) {
        try {
            Object arrayLength = scriptEngine.eval("__consoleLogs.length", bindings);
            if (arrayLength instanceof Number) {
                int length = ((Number) arrayLength).intValue();
                for (int i = 0; i < length; i++) {
                    Object logMessage = scriptEngine.eval("__consoleLogs[" + i + "]", bindings);
                    logConsoleMessage(logMessage);
                }
            }
        } catch (ScriptException e) {
            log.warn("Error processing console logs array: {}", e.getMessage());
        }
    }

    private void logConsoleMessage(Object logMessage) {
        if (logMessage != null) {
            String message = logMessage.toString();
            if (message.startsWith("[CONSOLE.ERROR]")) {
                log.error(message);
            } else if (message.startsWith("[CONSOLE.WARN]")) {
                log.warn(message);
            } else {
                log.info(message);
            }
        }
    }

    /**
     * Syncs JavaScript objects back to Java maps
     * Enhanced to handle new JavaScript objects and properties
     */
    @SuppressWarnings("null") // scriptEngine is validated before this method is called
    private void syncJavaScriptObjects(SimpleBindings bindings, Map<String, String> headers) {
        try {
            // Sync variables from JavaScript
            syncJavaScriptObjectToMap(scriptEngine, bindings, "__jsVariables", variables,
                    value -> value);

            // Sync headers from JavaScript
            syncJavaScriptObjectToMap(scriptEngine, bindings, "__jsHeaders", headers,
                    value -> value != null ? value.toString() : null);

            // Check for any remaining promises in the queue
            Object queueResult = scriptEngine.eval("__promiseQueue.length", bindings);
            if (queueResult instanceof Number && ((Number) queueResult).intValue() > 0) {
                log.warn("There are {} unprocessed promises in the queue", queueResult);
            }

            // Check for any errors that occurred during script execution
            Object lastError = scriptEngine.eval("__lastError", bindings);
            if (lastError != null && !"null".equals(lastError.toString())) {
                log.warn("Script execution had errors: {}", lastError);
            }

            // Debug logs
            log.info("Headers after script execution: {}", headers);
            log.info("Variables after script execution: {}", variables);
        } catch (Exception e) {
            log.warn("Error syncing JavaScript objects back to Java: {}", e.getMessage(), e);
        }
    }

    /**
     * Notifies script execution completion
     */
    private void notifyScriptComplete() {
        if (onScriptExecutionComplete != null) {
            onScriptExecutionComplete.run();
        }
    }

    /**
     * Handles script execution errors
     */
    private RuntimeException handleScriptError(Exception e, String context) {
        String message = context + ": " + e.getMessage();
        log.error(message, e);
        return new RuntimeException(message, e);
    }

    /**
     * Executes the pre-request script and applies any changes to the provided headers map
     *
     * @param headers The headers map to update with script-modified headers
     * @return CompletableFuture that completes when script execution is done
     */
    public CompletableFuture<Void> executeScript(Map<String, String> headers) {
        return CompletableFuture.runAsync(() -> {
            try {
                validateScriptEngine();

                String script = getScript();
                if (script == null || script.trim().isEmpty()) {
                    return;
                }

                SimpleBindings bindings = createScriptBindings(headers);
                executeScriptWithBindings(script, bindings);
                syncJavaScriptObjects(bindings, headers);
                notifyScriptComplete();
            } catch (ScriptException e) {
                throw handleScriptError(e, "Script execution failed");
            } catch (Exception e) {
                throw handleScriptError(e, "Unexpected script execution error");
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
     * Sets the environment manager
     * 
     * @param environmentManager the environment manager to use
     */
    public void setEnvironmentManager(EnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
    }
    
    /**
     * Resolves environment variables in a string.
     * Environment variables are referenced using the syntax {{variable_name}}.
     * 
     * @param input the input string to resolve
     * @return the resolved string with environment variables replaced
     */
    public String resolveEnvironmentVariables(String input) {
        if (input == null || input.isEmpty() || environmentManager == null) {
            return input;
        }
        
        return environmentManager.resolveVariables(input);
    }

    /**
     * Refreshes the syntax highlighting for the entire code area
     */
    private void refreshSyntaxHighlighting() {
        String text = codePreRequestScript.getText();
        codePreRequestScript.setStyleSpans(0, javaScriptColorize.computeHighlighting(text));
    }

    /**
     * Inserts a snippet to get a variable
     */
    @FXML
    public void insertGetVariableSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = """
                var value = pm.variables.get("variable_name");
                console.log("Variable value: " + value);
                """;
        codePreRequestScript.insertText(position, snippet);
        refreshSyntaxHighlighting();
    }

    /**
     * Inserts a snippet to set a variable
     */
    @FXML
    public void insertSetVariableSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = """
                pm.variables.set("variable_name", "variable_value");
                """;
        codePreRequestScript.insertText(position, snippet);
        refreshSyntaxHighlighting();
    }

    /**
     * Inserts a snippet to send a request
     */
    @FXML
    public void insertSendRequestSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = """
                pm.sendRequest("https://example.com/api", function (err, response) {
                    if (err) {
                        console.log(err);
                    } else {
                        console.log(response.json());
                        // Set a variable from the response
                        pm.variables.set("response_data", response.json().data);
                    }
                });
                """;
        codePreRequestScript.insertText(position, snippet);
        refreshSyntaxHighlighting();
    }

    /**
     * Inserts a snippet to modify headers
     */
    @FXML
    public void insertModifyHeadersSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = """
                // Add or modify a request header
                pm.request.headers["Custom-Header"] = "Header-Value";
                """;
        codePreRequestScript.insertText(position, snippet);
        refreshSyntaxHighlighting();
    }

    /**
     * Inserts a snippet to log a message
     */
    @FXML
    public void insertLogSnippet() {
        int position = codePreRequestScript.getCaretPosition();
        String snippet = """
                console.log("Your message here");
                """;
        codePreRequestScript.insertText(position, snippet);
        refreshSyntaxHighlighting();
    }

    /**
     * Inserts a complete example snippet
     */
    @FXML
    public void insertCompleteExampleSnippet() {
        String snippet = """
                // Example pre-request script
                // You can use the pm object to access variables and send requests
                
                // Set a variable
                pm.variables.set("api_key", "your-api-key");
                
                // Get a variable
                var apiKey = pm.variables.get("api_key");
                console.log("API Key: " + apiKey);
                
                // Add a custom header
                pm.request.headers["X-API-Key"] = apiKey;
                
                // Send a request to get authentication token
                pm.sendRequest("https://auth.example.com/token", function (err, response) {
                    if (err) {
                        console.log("Error getting token: " + err);
                    } else {
                        var token = response.json().token;
                        console.log("Got token: " + token);
                
                        // Store the token in a variable for the main request
                        pm.variables.set("auth_token", token);
                
                        // Add the token to the Authorization header
                        pm.request.headers["Authorization"] = "Bearer " + token;
                    }
                });
                """;
        codePreRequestScript.replaceText(snippet);
        refreshSyntaxHighlighting();
    }

    /**
     * Syncs a JavaScript object to a Java Map
     */
    @SuppressWarnings("unchecked")
    private void syncJavaScriptObjectToMap(ScriptEngine scriptEngine,
            SimpleBindings bindings,
            String jsObjectName,
            Map<String, ?> targetMap,
            Function<Object, Object> valueConverter) throws ScriptException {
        Object jsResult = scriptEngine.eval(jsObjectName, bindings);
        log.info("JavaScript {} result type: {}, value: {}", jsObjectName,
                jsResult.getClass().getName(), jsResult);

        Map<String, Object> typedMap = (Map<String, Object>) targetMap;

        if (jsResult instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> jsMap = (java.util.Map<String, Object>) jsResult;
            typedMap.clear();
            for (Map.Entry<String, Object> entry : jsMap.entrySet()) {
                if (entry.getValue() != null) {
                    Object value = valueConverter.apply(entry.getValue());
                    if (value != null) {
                        typedMap.put(entry.getKey(), value);
                    }
                }
            }
            log.info("Synced {} from JavaScript: {}", jsObjectName, typedMap);
        } else {
            // Try to access as object properties using eval
            String jsKeys = (String) scriptEngine.eval("Object.keys(" + jsObjectName + ").join(',')", bindings);
            log.info("JavaScript {} keys: {}", jsObjectName, jsKeys);

            if (jsKeys != null && !jsKeys.isEmpty()) {
                typedMap.clear();
                for (String key : jsKeys.split(",")) {
                    if (!key.trim().isEmpty()) {
                        Object value = scriptEngine.eval(jsObjectName + "['" + key.trim() + "']", bindings);
                        if (value != null) {
                            Object convertedValue = valueConverter.apply(value);
                            if (convertedValue != null) {
                                typedMap.put(key.trim(), convertedValue);
                                log.info("Synced {}: {} = {}", jsObjectName, key.trim(), convertedValue);
                            }
                        }
                    }
                }
            }
        }
    }
}
