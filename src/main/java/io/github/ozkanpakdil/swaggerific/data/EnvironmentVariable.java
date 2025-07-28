package io.github.ozkanpakdil.swaggerific.data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a single environment variable with a key and value.
 */
public class EnvironmentVariable implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String key;
    private String value;
    private boolean isSecret;

    /**
     * Default constructor for serialization
     */
    public EnvironmentVariable() {
    }

    /**
     * Creates a new environment variable with the specified key and value.
     *
     * @param key   the variable key
     * @param value the variable value
     */
    public EnvironmentVariable(String key, String value) {
        this(key, value, false);
    }

    /**
     * Creates a new environment variable with the specified key, value, and secret flag.
     *
     * @param key      the variable key
     * @param value    the variable value
     * @param isSecret whether the variable contains sensitive information
     */
    public EnvironmentVariable(String key, String value, boolean isSecret) {
        this.key = key;
        this.value = value;
        this.isSecret = isSecret;
    }

    /**
     * Gets the variable key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the variable key.
     *
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the variable value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the variable value.
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Checks if the variable contains sensitive information.
     *
     * @return true if the variable is secret, false otherwise
     */
    public boolean isSecret() {
        return isSecret;
    }

    /**
     * Sets whether the variable contains sensitive information.
     *
     * @param secret the secret flag to set
     */
    public void setSecret(boolean secret) {
        isSecret = secret;
    }

    @Override
    public String toString() {
        return key + "=" + (isSecret ? "********" : value);
    }
}