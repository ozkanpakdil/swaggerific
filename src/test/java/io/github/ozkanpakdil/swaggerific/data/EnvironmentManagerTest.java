package io.github.ozkanpakdil.swaggerific.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EnvironmentManager class.
 * These tests verify that environment variables are correctly managed and resolved.
 */
class EnvironmentManagerTest {

    private EnvironmentManager environmentManager;

    @BeforeEach
    void setUp() {
        environmentManager = new EnvironmentManager();
        
        // Clear any existing environments
        environmentManager.clear();
        
        // Create test environments
        Environment dev = new Environment("Development", "Development environment");
        dev.setVariable("baseUrl", "https://dev-api.example.com");
        dev.setVariable("apiKey", "dev-api-key-123");
        dev.setVariable("timeout", "5000");
        dev.setVariable("secretToken", "dev-secret-token", true);
        
        Environment staging = new Environment("Staging", "Staging environment");
        staging.setVariable("baseUrl", "https://staging-api.example.com");
        staging.setVariable("apiKey", "staging-api-key-456");
        staging.setVariable("timeout", "3000");
        staging.setVariable("secretToken", "staging-secret-token", true);
        
        Environment prod = new Environment("Production", "Production environment");
        prod.setVariable("baseUrl", "https://api.example.com");
        prod.setVariable("apiKey", "prod-api-key-789");
        prod.setVariable("timeout", "2000");
        prod.setVariable("secretToken", "prod-secret-token", true);
        
        // Add environments to manager
        environmentManager.addEnvironment(dev);
        environmentManager.addEnvironment(staging);
        environmentManager.addEnvironment(prod);
        
        // Set Development as active environment
        environmentManager.setActiveEnvironment("Development");
    }

    @Test
    void testEnvironmentCreationAndRetrieval() {
        // Test environment count
        assertEquals(3, environmentManager.size());
        
        // Test getting all environments
        assertEquals(3, environmentManager.getAllEnvironments().size());
        
        // Test getting environment by name
        Optional<Environment> dev = environmentManager.getEnvironment("Development");
        assertTrue(dev.isPresent());
        assertEquals("Development", dev.get().getName());
        assertEquals("Development environment", dev.get().getDescription());
        
        // Test active environment
        Optional<Environment> active = environmentManager.getActiveEnvironment();
        assertTrue(active.isPresent());
        assertEquals("Development", active.get().getName());
    }
    
    @Test
    void testEnvironmentVariables() {
        // Test getting variable from active environment
        Optional<String> baseUrl = environmentManager.getVariableValue("baseUrl");
        assertTrue(baseUrl.isPresent());
        assertEquals("https://dev-api.example.com", baseUrl.get());
        
        // Test switching active environment
        environmentManager.setActiveEnvironment("Staging");
        baseUrl = environmentManager.getVariableValue("baseUrl");
        assertTrue(baseUrl.isPresent());
        assertEquals("https://staging-api.example.com", baseUrl.get());
        
        // Test getting variable that doesn't exist
        Optional<String> nonExistent = environmentManager.getVariableValue("nonExistentVar");
        assertFalse(nonExistent.isPresent());
    }
    
    @Test
    void testVariableResolution() {
        // Test simple variable resolution
        String input = "Connect to {{baseUrl}}/api with key {{apiKey}}";
        String expected = "Connect to https://dev-api.example.com/api with key dev-api-key-123";
        assertEquals(expected, environmentManager.resolveVariables(input));
        
        // Test variable that doesn't exist (should be left as is)
        input = "Value: {{nonExistentVar}}";
        expected = "Value: {{nonExistentVar}}";
        assertEquals(expected, environmentManager.resolveVariables(input));
        
        // Test multiple variables
        input = "URL: {{baseUrl}}, Key: {{apiKey}}, Timeout: {{timeout}}";
        expected = "URL: https://dev-api.example.com, Key: dev-api-key-123, Timeout: 5000";
        assertEquals(expected, environmentManager.resolveVariables(input));
        
        // Test with no variables
        input = "Plain text with no variables";
        assertEquals(input, environmentManager.resolveVariables(input));
        
        // Test with empty string
        input = "";
        assertEquals(input, environmentManager.resolveVariables(input));
        
        // Test with null
        assertNull(environmentManager.resolveVariables(null));
    }
    
    @Test
    void testEnvironmentSwitching() {
        // Test variable resolution with Development environment
        String input = "{{baseUrl}}/api";
        String devResult = "https://dev-api.example.com/api";
        assertEquals(devResult, environmentManager.resolveVariables(input));
        
        // Switch to Staging
        environmentManager.setActiveEnvironment("Staging");
        String stagingResult = "https://staging-api.example.com/api";
        assertEquals(stagingResult, environmentManager.resolveVariables(input));
        
        // Switch to Production
        environmentManager.setActiveEnvironment("Production");
        String prodResult = "https://api.example.com/api";
        assertEquals(prodResult, environmentManager.resolveVariables(input));
    }
    
    @Test
    void testEnvironmentManagement() {
        // Test adding a new environment
        Environment test = new Environment("Test", "Test environment");
        test.setVariable("baseUrl", "https://test-api.example.com");
        
        assertTrue(environmentManager.addEnvironment(test));
        assertEquals(4, environmentManager.size());
        
        // Test updating an environment
        test.setDescription("Updated test environment");
        test.setVariable("newVar", "newValue");
        
        assertTrue(environmentManager.updateEnvironment("Test", test));
        
        Optional<Environment> updated = environmentManager.getEnvironment("Test");
        assertTrue(updated.isPresent());
        assertEquals("Updated test environment", updated.get().getDescription());
        
        // Test removing an environment
        assertTrue(environmentManager.removeEnvironment("Test"));
        assertEquals(3, environmentManager.size());
        assertFalse(environmentManager.getEnvironment("Test").isPresent());
        
        // Test cannot remove active environment
        assertFalse(environmentManager.removeEnvironment("Development"));
    }
    
    @Test
    void testEdgeCases() {
        // Test variable with partial match
        String input = "This is a {{partialVar";
        assertEquals(input, environmentManager.resolveVariables(input));
        
        // Test variable with no closing brackets
        input = "This is a {{missingClosingBracket";
        assertEquals(input, environmentManager.resolveVariables(input));
        
        // Test empty variable name
        input = "This is an {{}} empty variable";
        assertEquals(input, environmentManager.resolveVariables(input));
        
        // Test adjacent variables
        environmentManager.getActiveEnvironment().get().setVariable("var1", "Hello");
        environmentManager.getActiveEnvironment().get().setVariable("var2", "World");
        input = "{{var1}}{{var2}}";
        assertEquals("HelloWorld", environmentManager.resolveVariables(input));
    }
}