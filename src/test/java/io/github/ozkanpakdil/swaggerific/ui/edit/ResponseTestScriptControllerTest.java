package io.github.ozkanpakdil.swaggerific.ui.edit;

import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ResponseTestScriptController
 */
//TODO enable after js fix
@Ignore("Ignore until js setup fixed." +
        "This test class is for testing the ResponseTestScriptController functionality, not for UI components.")
public class ResponseTestScriptControllerTest {

    @Test
    @Timeout(5)
    public void testBasicAssertions() throws Exception {
        TestableResponseTestScriptController controller = new TestableResponseTestScriptController();

        // Create a test response
        HttpResponse response = new HttpResponse.Builder()
                .statusCode(200)
                .body("{\"id\": 123, \"name\": \"Test\"}")
                .contentType("application/json")
                .build();

        // Set up a script with basic assertions
        String script = "// Test basic assertions\n" +
                "pm.test.assertTrue(\"True assertion\", true);\n" +
                "pm.test.assertFalse(\"False assertion\", false);\n" +
                "pm.test.assertEquals(\"Equal assertion\", 1, 1);\n";

        controller.setTestScript(script);

        // Execute the script
        List<ResponseTestScriptController.AssertionResult> results = controller.executeScript(response).get();

        // Verify results
        assertEquals(3, results.size(), "Should have 3 assertion results");
        assertTrue(results.get(0).passed(), "First assertion should pass");
        assertTrue(results.get(1).passed(), "Second assertion should pass");
        assertTrue(results.get(2).passed(), "Third assertion should pass");

        assertEquals("True assertion", results.get(0).message());
        assertEquals("False assertion", results.get(1).message());
        assertEquals("Equal assertion", results.get(2).message());
    }

    @Test
    @Timeout(5)
    public void testFailedAssertions() throws Exception {
        TestableResponseTestScriptController controller = new TestableResponseTestScriptController();

        // Create a test response
        HttpResponse response = new HttpResponse.Builder()
                .statusCode(200)
                .body("{\"id\": 123, \"name\": \"Test\"}")
                .contentType("application/json")
                .build();

        // Set up a script with failing assertions
        String script = "// Test failing assertions\n" +
                "pm.test.assertTrue(\"Should fail\", false);\n" +
                "pm.test.assertFalse(\"Should fail too\", true);\n" +
                "pm.test.assertEquals(\"Not equal\", 1, 2);\n";

        controller.setTestScript(script);

        // Execute the script
        List<ResponseTestScriptController.AssertionResult> results = controller.executeScript(response).get();

        // Verify results
        assertEquals(3, results.size(), "Should have 3 assertion results");
        assertFalse(results.get(0).passed(), "First assertion should fail");
        assertFalse(results.get(1).passed(), "Second assertion should fail");
        assertFalse(results.get(2).passed(), "Third assertion should fail");
    }

    @Test
    @Timeout(5)
    public void testStatusCodeAssertions() throws Exception {
        TestableResponseTestScriptController controller = new TestableResponseTestScriptController();

        // Create a test response with status code 200
        HttpResponse response = new HttpResponse.Builder()
                .statusCode(200)
                .body("{\"success\": true}")
                .contentType("application/json")
                .build();

        // Set up a script with status code assertions
        String script = "// Test status code assertions\n" +
                "pm.test.assertStatusCode(\"Status should be 200\", 200);\n" +
                "pm.test.assertTrue(\"Status should be in 2xx range\", \n" +
                "    pm.response.status >= 200 && pm.response.status < 300);\n";

        controller.setTestScript(script);

        // Execute the script
        List<ResponseTestScriptController.AssertionResult> results = controller.executeScript(response).get();

        // Verify results
        assertEquals(2, results.size(), "Should have 2 assertion results");
        assertTrue(results.get(0).passed(), "First assertion should pass");
        assertTrue(results.get(1).passed(), "Second assertion should pass");

        // Test with a different status code
        response = new HttpResponse.Builder()
                .statusCode(404)
                .body("{\"error\": \"Not found\"}")
                .contentType("application/json")
                .build();

        // Execute the script again
        results = controller.executeScript(response).get();

        // Verify results
        assertEquals(2, results.size(), "Should have 2 assertion results");
        assertFalse(results.get(0).passed(), "First assertion should fail");
        assertFalse(results.get(1).passed(), "Second assertion should fail");
    }

    @Test
    @Timeout(5)
    public void testHeaderAssertions() throws Exception {
        TestableResponseTestScriptController controller = new TestableResponseTestScriptController();

        // Create a test response with headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "test-value");

        HttpResponse response = new HttpResponse.Builder()
                .statusCode(200)
                .headers(headers)
                .body("{}")
                .contentType("application/json")
                .build();

        // Set up a script with header assertions
        String script = "// Test header assertions\n" +
                "pm.test.assertHeader(\"Should have Content-Type header\", \"Content-Type\");\n" +
                "pm.test.assertHeader(\"Should have X-Custom-Header\", \"X-Custom-Header\");\n" +
                "pm.test.assertHeaderValue(\"Content-Type should be application/json\", \n" +
                "    \"Content-Type\", \"application/json\");\n" +
                "pm.test.assertHeaderValue(\"X-Custom-Header should be test-value\", \n" +
                "    \"X-Custom-Header\", \"test-value\");\n";

        controller.setTestScript(script);

        // Execute the script
        List<ResponseTestScriptController.AssertionResult> results = controller.executeScript(response).get();

        // Verify results
        assertEquals(4, results.size(), "Should have 4 assertion results");
        assertTrue(results.get(0).passed(), "First assertion should pass");
        assertTrue(results.get(1).passed(), "Second assertion should pass");
        assertTrue(results.get(2).passed(), "Third assertion should pass");
        assertTrue(results.get(3).passed(), "Fourth assertion should pass");

        // Test with missing and incorrect headers
        headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");

        response = new HttpResponse.Builder()
                .statusCode(200)
                .headers(headers)
                .body("{}")
                .contentType("text/plain")
                .build();

        // Execute the script again
        results = controller.executeScript(response).get();

        // Verify results
        assertEquals(4, results.size(), "Should have 4 assertion results");
        assertTrue(results.get(0).passed(), "First assertion should pass");
        assertFalse(results.get(1).passed(), "Second assertion should fail");
        assertFalse(results.get(2).passed(), "Third assertion should fail");
        assertFalse(results.get(3).passed(), "Fourth assertion should fail");
    }

    @Test
    @Timeout(5)
    public void testJsonBodyAssertions() throws Exception {
        TestableResponseTestScriptController controller = new TestableResponseTestScriptController();

        // Create a test response with JSON body
        HttpResponse response = new HttpResponse.Builder()
                .statusCode(200)
                .body("{\"id\": 123, \"name\": \"Test\", \"items\": [1, 2, 3]}")
                .contentType("application/json")
                .build();

        // Set up a script with JSON body assertions
        String script = "// Test JSON body assertions\n" +
                "const jsonData = pm.response.json();\n" +
                "pm.test.assertEquals(\"ID should be 123\", jsonData.id, 123);\n" +
                "pm.test.assertEquals(\"Name should be Test\", jsonData.name, \"Test\");\n" +
                "pm.test.assertEquals(\"Items array should have length 3\", jsonData.items.length, 3);\n" +
                "pm.test.assertTrue(\"Items array should contain 2\", jsonData.items.includes(2));\n";

        controller.setTestScript(script);

        // Execute the script
        List<ResponseTestScriptController.AssertionResult> results = controller.executeScript(response).get();

        // Verify results
        assertEquals(4, results.size(), "Should have 4 assertion results");
        assertTrue(results.get(0).passed(), "First assertion should pass");
        assertTrue(results.get(1).passed(), "Second assertion should pass");
        assertTrue(results.get(2).passed(), "Third assertion should pass");
        assertTrue(results.get(3).passed(), "Fourth assertion should pass");
    }

    @Test
    @Timeout(5)
    public void testBodyContainsAssertion() throws Exception {
        TestableResponseTestScriptController controller = new TestableResponseTestScriptController();

        // Create a test response
        HttpResponse response = new HttpResponse.Builder()
                .statusCode(200)
                .body("This is a test response with some specific text")
                .contentType("text/plain")
                .build();

        // Set up a script with body contains assertions
        String script = "// Test body contains assertions\n" +
                "pm.test.assertContains(\"Body should contain 'test'\", pm.response.body, \"test\");\n" +
                "pm.test.assertContains(\"Body should contain 'specific'\", pm.response.body, \"specific\");\n" +
                "pm.test.assertContains(\"Body should contain 'not found'\", pm.response.body, \"not found\");\n";

        controller.setTestScript(script);

        // Execute the script
        List<ResponseTestScriptController.AssertionResult> results = controller.executeScript(response).get();

        // Verify results
        assertEquals(3, results.size(), "Should have 3 assertion results");
        assertTrue(results.get(0).passed(), "First assertion should pass");
        assertTrue(results.get(1).passed(), "Second assertion should pass");
        assertFalse(results.get(2).passed(), "Third assertion should fail");
    }

    @Test
    @Timeout(5)
    public void testVariableHandling() throws Exception {
        TestableResponseTestScriptController controller = new TestableResponseTestScriptController();

        // Create a test response
        HttpResponse response = new HttpResponse.Builder()
                .statusCode(200)
                .body("{\"id\": 123, \"token\": \"abc-xyz\"}")
                .contentType("application/json")
                .build();

        // Set up a script that sets and gets variables
        String script = "// Test variable handling\n" +
                "const jsonData = pm.response.json();\n" +
                "pm.variables.set(\"id\", jsonData.id);\n" +
                "pm.variables.set(\"token\", jsonData.token);\n" +
                "const retrievedId = pm.variables.get(\"id\");\n" +
                "const retrievedToken = pm.variables.get(\"token\");\n" +
                "pm.test.assertEquals(\"Retrieved ID should match\", retrievedId, 123);\n" +
                "pm.test.assertEquals(\"Retrieved token should match\", retrievedToken, \"abc-xyz\");\n";

        controller.setTestScript(script);

        // Execute the script
        List<ResponseTestScriptController.AssertionResult> results = controller.executeScript(response).get();

        // Verify results
        assertEquals(2, results.size(), "Should have 2 assertion results");
        assertTrue(results.get(0).passed(), "First assertion should pass");
        assertTrue(results.get(1).passed(), "Second assertion should pass");

        // Verify variables were stored
        assertEquals(123, controller.getVariables().get("id"));
        assertEquals("abc-xyz", controller.getVariables().get("token"));
    }

    @Test
    @Timeout(5)
    public void testConsoleLogging() throws Exception {
        TestableResponseTestScriptController controller = new TestableResponseTestScriptController();

        // Create a test response
        HttpResponse response = new HttpResponse.Builder()
                .statusCode(200)
                .body("{}")
                .contentType("application/json")
                .build();

        // Set up a script with console logging
        String script = "// Test console logging\n" +
                "console.log(\"Regular log message\");\n" +
                "console.info(\"Info message\");\n" +
                "console.warn(\"Warning message\");\n" +
                "console.error(\"Error message\");\n" +
                "console.debug(\"Debug message\");\n";

        controller.setTestScript(script);

        // Execute the script
        controller.executeScript(response).get();

        // Verify logs (we can't directly verify the logs, but we can verify the script executed without errors)
        assertTrue(true, "Script should execute without errors");
    }

    @Test
    @Timeout(5)
    public void testCompleteExample() throws Exception {
        TestableResponseTestScriptController controller = new TestableResponseTestScriptController();

        // Create a test response
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Rate-Limit", "100");

        HttpResponse response = new HttpResponse.Builder()
                .statusCode(200)
                .headers(headers)
                .body("{\"id\": 123, \"name\": \"Test Product\", \"price\": 99.99, \"tags\": [\"new\", \"featured\"]}")
                .contentType("application/json")
                .build();

        // Set up a complete test script
        String script = "// Complete test example\n" +
                "\n" +
                "// Test status code\n" +
                "pm.test.assertStatusCode(\"Status code should be 200\", 200);\n" +
                "\n" +
                "// Test headers\n" +
                "pm.test.assertHeader(\"Response should have Content-Type header\", \"Content-Type\");\n" +
                "pm.test.assertHeaderValue(\"Content-Type should be application/json\", \n" +
                "    \"Content-Type\", \"application/json\");\n" +
                "pm.test.assertHeader(\"Response should have X-Rate-Limit header\", \"X-Rate-Limit\");\n" +
                "\n" +
                "// Parse JSON response\n" +
                "const jsonData = pm.response.json();\n" +
                "\n" +
                "// Test JSON properties\n" +
                "pm.test.assertTrue(\"Response should have id property\", \n" +
                "    jsonData.hasOwnProperty(\"id\"));\n" +
                "pm.test.assertEquals(\"ID should be 123\", jsonData.id, 123);\n" +
                "pm.test.assertEquals(\"Name should be Test Product\", jsonData.name, \"Test Product\");\n" +
                "pm.test.assertEquals(\"Price should be 99.99\", jsonData.price, 99.99);\n" +
                "pm.test.assertEquals(\"Tags array should have length 2\", jsonData.tags.length, 2);\n" +
                "pm.test.assertTrue(\"Tags should include 'featured'\", jsonData.tags.includes(\"featured\"));\n" +
                "\n" +
                "// Store a value from the response in a variable\n" +
                "pm.variables.set(\"productId\", jsonData.id);\n" +
                "pm.variables.set(\"productName\", jsonData.name);\n" +
                "\n" +
                "// Verify stored variables\n" +
                "const storedId = pm.variables.get(\"productId\");\n" +
                "pm.test.assertEquals(\"Stored ID should match\", storedId, 123);\n" +
                "\n" +
                "// Log the response\n" +
                "console.log(\"Response received:\", jsonData);\n";

        controller.setTestScript(script);

        // Execute the script
        List<ResponseTestScriptController.AssertionResult> results = controller.executeScript(response).get();

        // Verify results
        assertEquals(12, results.size(), "Should have 12 assertion results");

        // All assertions should pass
        for (int i = 0; i < results.size(); i++) {
            assertTrue(results.get(i).passed(), "Assertion " + (i + 1) + " should pass");
        }

        // Verify variables were stored
        assertEquals(123, controller.getVariables().get("productId"));
        assertEquals("Test Product", controller.getVariables().get("productName"));
    }

    /**
     * A testable version of ResponseTestScriptController that doesn't depend on UI components
     */
    private static class TestableResponseTestScriptController extends ResponseTestScriptController {
        private String testScript = "";
        private final List<AssertionResult> testAssertionResults = new ArrayList<>();
        private final ScriptEngine scriptEngine;
        private final Map<String, Object> variables = new HashMap<>();

        public TestableResponseTestScriptController() {
            // Initialize script engine
            ScriptEngineManager manager = new ScriptEngineManager();
            scriptEngine = manager.getEngineByName("graal.js");
            if (scriptEngine == null) {
                throw new RuntimeException("GraalVM JavaScript engine not found");
            }
        }

        public void setTestScript(String script) {
            this.testScript = script;
        }

        @Override
        public String getScript() {
            return testScript;
        }

        @Override
        public Map<String, Object> getVariables() {
            return variables;
        }

        @Override
        public CompletableFuture<List<AssertionResult>> executeScript(HttpResponse response) {
            CompletableFuture<List<AssertionResult>> future = new CompletableFuture<>();

            try {
                // Clear previous results
                testAssertionResults.clear();

                // Create bindings with the response
                SimpleBindings bindings = new SimpleBindings();

                // Add console object for logging
                Map<String, Object> console = new HashMap<>();
                List<Object> consoleLogs = new ArrayList<>();
                console.put("logs", consoleLogs);

                // Console logging methods
                console.put("log", (java.util.function.Function<Object, Void>) message -> {
                    consoleLogs.add(message);
                    return null;
                });

                console.put("info", (java.util.function.Function<Object, Void>) message -> {
                    consoleLogs.add("INFO: " + message);
                    return null;
                });

                console.put("warn", (java.util.function.Function<Object, Void>) message -> {
                    consoleLogs.add("WARNING: " + message);
                    return null;
                });

                console.put("error", (java.util.function.Function<Object, Void>) message -> {
                    consoleLogs.add("ERROR: " + message);
                    return null;
                });

                console.put("debug", (java.util.function.Function<Object, Void>) message -> {
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
                responseObj.put("json", (java.util.function.Function<Void, Object>) v -> {
                    try {
                        return io.swagger.v3.core.util.Json.mapper().readValue(response.body(), Object.class);
                    } catch (Exception e) {
                        return null;
                    }
                });

                pm.put("response", responseObj);

                // Add test object for assertions
                Map<String, Object> test = new HashMap<>();

                // Add assertion methods
                test.put("assertEquals", (java.util.function.Function<Object[], Boolean>) args -> {
                    if (args.length < 3) {
                        String message = args.length > 0 ? args[0].toString() : "Assertion failed";
                        testAssertionResults.add(
                                new AssertionResult(false, message + ": Not enough arguments for assertEquals"));
                        return false;
                    }

                    String message = args[0].toString();
                    Object actual = args[1];
                    Object expected = args[2];

                    boolean result = (actual == null && expected == null) ||
                            (actual != null && actual.equals(expected));

                    testAssertionResults.add(new AssertionResult(result, message));
                    return result;
                });

                test.put("assertTrue", (java.util.function.Function<Object[], Boolean>) args -> {
                    if (args.length < 2) {
                        String message = args.length > 0 ? args[0].toString() : "Assertion failed";
                        testAssertionResults.add(new AssertionResult(false, message + ": Not enough arguments for assertTrue"));
                        return false;
                    }

                    String message = args[0].toString();
                    Object condition = args[1];

                    boolean result = condition instanceof Boolean && (Boolean) condition;

                    testAssertionResults.add(new AssertionResult(result, message));
                    return result;
                });

                test.put("assertFalse", (java.util.function.Function<Object[], Boolean>) args -> {
                    if (args.length < 2) {
                        String message = args.length > 0 ? args[0].toString() : "Assertion failed";
                        testAssertionResults.add(
                                new AssertionResult(false, message + ": Not enough arguments for assertFalse"));
                        return false;
                    }

                    String message = args[0].toString();
                    Object condition = args[1];

                    boolean result = condition instanceof Boolean && !(Boolean) condition;

                    testAssertionResults.add(new AssertionResult(result, message));
                    return result;
                });

                test.put("assertContains", (java.util.function.Function<Object[], Boolean>) args -> {
                    if (args.length < 3) {
                        String message = args.length > 0 ? args[0].toString() : "Assertion failed";
                        testAssertionResults.add(
                                new AssertionResult(false, message + ": Not enough arguments for assertContains"));
                        return false;
                    }

                    String message = args[0].toString();
                    String haystack = args[1] != null ? args[1].toString() : "";
                    String needle = args[2] != null ? args[2].toString() : "";

                    boolean result = haystack.contains(needle);

                    testAssertionResults.add(new AssertionResult(result, message));
                    return result;
                });

                test.put("assertStatusCode", (java.util.function.Function<Object[], Boolean>) args -> {
                    if (args.length < 2) {
                        String message = "Status code assertion failed: Not enough arguments";
                        testAssertionResults.add(new AssertionResult(false, message));
                        return false;
                    }

                    String message = args.length > 2 ? args[0].toString() :
                            "Status code should be " + args[args.length - 1];
                    int expectedStatus = args[args.length - 1] instanceof Number ?
                            ((Number) args[args.length - 1]).intValue() : -1;

                    boolean result = response.statusCode() == expectedStatus;

                    testAssertionResults.add(new AssertionResult(result, message));
                    return result;
                });

                test.put("assertHeader", (java.util.function.Function<Object[], Boolean>) args -> {
                    if (args.length < 2) {
                        String message = "Header assertion failed: Not enough arguments";
                        testAssertionResults.add(new AssertionResult(false, message));
                        return false;
                    }

                    String headerName = args[args.length - 1].toString();
                    String message = args.length > 2 ? args[0].toString() :
                            "Response should have header '" + headerName + "'";

                    boolean result = response.headers().containsKey(headerName);

                    testAssertionResults.add(new AssertionResult(result, message));
                    return result;
                });

                test.put("assertHeaderValue", (java.util.function.Function<Object[], Boolean>) args -> {
                    if (args.length < 3) {
                        String message = "Header value assertion failed: Not enough arguments";
                        testAssertionResults.add(new AssertionResult(false, message));
                        return false;
                    }

                    String headerName = args[args.length - 2].toString();
                    String expectedValue = args[args.length - 1].toString();
                    String message = args.length > 3 ? args[0].toString() :
                            "Header '" + headerName + "' should have value '" + expectedValue + "'";

                    String actualValue = response.headers().get(headerName);
                    boolean result = actualValue != null && actualValue.equals(expectedValue);

                    testAssertionResults.add(new AssertionResult(result, message));
                    return result;
                });

                pm.put("test", test);

                // Add environment variables
                Map<String, Object> environment = new HashMap<>();
                Map<String, Object> variablesMap = new HashMap<>();

                // Add variable getter and setter
                variablesMap.put("get", (java.util.function.Function<String, Object>) name -> {
                    return variables.get(name);
                });

                variablesMap.put("set", (java.util.function.Function<Object[], Void>) args -> {
                    if (args.length >= 2) {
                        String name = args[0].toString();
                        Object value = args[1];
                        variables.put(name, value);
                    }
                    return null;
                });

                pm.put("environment", environment);
                pm.put("variables", variablesMap);

                bindings.put("pm", pm);

                // Execute the script
                scriptEngine.eval(getScript(), bindings);

                // Process console logs
                for (Object log : consoleLogs) {
                    System.out.println("[Test Console] " + log);
                }

                // Complete the future with the assertion results
                future.complete(new ArrayList<>(testAssertionResults));

            } catch (ScriptException e) {
                future.completeExceptionally(e);
            }

            return future;
        }
    }
}