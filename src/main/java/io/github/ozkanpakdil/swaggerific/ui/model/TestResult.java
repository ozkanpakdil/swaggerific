package io.github.ozkanpakdil.swaggerific.ui.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Model class for test results displayed in the UI.
 */
public class TestResult {
    private final SimpleBooleanProperty passed;
    private final SimpleStringProperty message;
    private final SimpleStringProperty status;

    /**
     * Creates a new test result.
     *
     * @param passed whether the test passed
     * @param message the test message
     */
    public TestResult(boolean passed, String message) {
        this.passed = new SimpleBooleanProperty(passed);
        this.message = new SimpleStringProperty(message);
        this.status = new SimpleStringProperty(passed ? "✓" : "✗");
    }

    /**
     * Gets whether the test passed.
     *
     * @return whether the test passed
     */
    public boolean isPassed() {
        return passed.get();
    }

    /**
     * Gets the passed property.
     *
     * @return the passed property
     */
    public SimpleBooleanProperty passedProperty() {
        return passed;
    }

    /**
     * Gets the test message.
     *
     * @return the test message
     */
    public String getMessage() {
        return message.get();
    }

    /**
     * Gets the message property.
     *
     * @return the message property
     */
    public SimpleStringProperty messageProperty() {
        return message;
    }

    /**
     * Gets the status symbol (✓ for passed, ✗ for failed).
     *
     * @return the status symbol
     */
    public String getStatus() {
        return status.get();
    }

    /**
     * Gets the status property.
     *
     * @return the status property
     */
    public SimpleStringProperty statusProperty() {
        return status;
    }
}