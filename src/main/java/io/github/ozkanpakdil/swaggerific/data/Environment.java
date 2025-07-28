package io.github.ozkanpakdil.swaggerific.data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an environment with a collection of environment variables.
 * An environment can be development, staging, production, etc.
 */
public class Environment implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private final Map<String, EnvironmentVariable> variables = new HashMap<>();
    private boolean isActive;

    /**
     * Default constructor for serialization
     */
    public Environment() {
    }

    /**
     * Creates a new environment with the specified name.
     *
     * @param name the environment name
     */
    public Environment(String name) {
        this(name, "");
    }

    /**
     * Creates a new environment with the specified name and description.
     *
     * @param name        the environment name
     * @param description the environment description
     */
    public Environment(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the environment name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the environment name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the environment description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the environment description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Checks if this environment is currently active.
     *
     * @return true if the environment is active, false otherwise
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets whether this environment is currently active.
     *
     * @param active the active flag to set
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Gets a variable by key.
     *
     * @param key the variable key
     * @return an Optional containing the variable if found, or empty if not found
     */
    public Optional<EnvironmentVariable> getVariable(String key) {
        return Optional.ofNullable(variables.get(key));
    }

    /**
     * Gets the value of a variable by key.
     *
     * @param key the variable key
     * @return an Optional containing the variable value if found, or empty if not found
     */
    public Optional<String> getVariableValue(String key) {
        return getVariable(key).map(EnvironmentVariable::getValue);
    }

    /**
     * Sets a variable with the specified key and value.
     *
     * @param key   the variable key
     * @param value the variable value
     * @return the environment variable that was set
     */
    public EnvironmentVariable setVariable(String key, String value) {
        return setVariable(key, value, false);
    }

    /**
     * Sets a variable with the specified key, value, and secret flag.
     *
     * @param key      the variable key
     * @param value    the variable value
     * @param isSecret whether the variable contains sensitive information
     * @return the environment variable that was set
     */
    public EnvironmentVariable setVariable(String key, String value, boolean isSecret) {
        EnvironmentVariable variable = new EnvironmentVariable(key, value, isSecret);
        variables.put(key, variable);
        return variable;
    }

    /**
     * Removes a variable by key.
     *
     * @param key the variable key
     * @return true if the variable was removed, false if it wasn't found
     */
    public boolean removeVariable(String key) {
        return variables.remove(key) != null;
    }

    /**
     * Gets all variables in this environment.
     *
     * @return a list of all environment variables
     */
    public List<EnvironmentVariable> getAllVariables() {
        return new ArrayList<>(variables.values());
    }

    /**
     * Gets the number of variables in this environment.
     *
     * @return the number of variables
     */
    public int size() {
        return variables.size();
    }

    /**
     * Clears all variables from this environment.
     */
    public void clear() {
        variables.clear();
    }

    @Override
    public String toString() {
        return name;
    }
}