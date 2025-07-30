package io.github.ozkanpakdil.swaggerific.data;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages multiple environments and provides functionality to switch between them. Also handles persistence of environments.
 */
public class EnvironmentManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger log = (Logger) LoggerFactory.getLogger(EnvironmentManager.class);

    public static final String ENV_SETTINGS = System.getProperty(
            "user.home") + File.separator + ".swaggerific" + File.separator + "env_settings.ser";

    private final Map<String, Environment> environments = new HashMap<>();
    private String activeEnvironmentName;

    /**
     * Default constructor
     */
    public EnvironmentManager() {
        // Create default environments if none exist
        if (environments.isEmpty()) {
            createDefaultEnvironments();
        }
    }

    /**
     * Creates default environments (Development, Staging, Production)
     */
    private void createDefaultEnvironments() {
        Environment dev = new Environment("Development", "Development environment");
        Environment staging = new Environment("Staging", "Staging environment");
        Environment production = new Environment("Production", "Production environment");

        addEnvironment(dev);
        addEnvironment(staging);
        addEnvironment(production);

        // Set Development as the active environment by default
        setActiveEnvironment("Development");
    }

    /**
     * Gets all environments.
     *
     * @return a list of all environments
     */
    public List<Environment> getAllEnvironments() {
        return new ArrayList<>(environments.values());
    }

    /**
     * Gets an environment by name.
     *
     * @param name the environment name
     * @return an Optional containing the environment if found, or empty if not found
     */
    public Optional<Environment> getEnvironment(String name) {
        return Optional.ofNullable(environments.get(name));
    }

    /**
     * Gets the currently active environment.
     *
     * @return an Optional containing the active environment if set, or empty if not set
     */
    public Optional<Environment> getActiveEnvironment() {
        return getEnvironment(activeEnvironmentName);
    }

    /**
     * Sets the active environment by name.
     *
     * @param name the name of the environment to set as active
     * @return true if the environment was found and set as active, false otherwise
     */
    public boolean setActiveEnvironment(String name) {
        Optional<Environment> environment = getEnvironment(name);
        if (environment.isPresent()) {
            // Deactivate the current active environment
            getActiveEnvironment().ifPresent(env -> env.setActive(false));

            // Activate the new environment
            environment.get().setActive(true);
            activeEnvironmentName = name;
            return true;
        }
        return false;
    }

    /**
     * Adds a new environment.
     *
     * @param environment the environment to add
     * @return true if the environment was added, false if an environment with the same name already exists
     */
    public boolean addEnvironment(Environment environment) {
        if (environments.containsKey(environment.getName())) {
            return false;
        }
        environments.put(environment.getName(), environment);
        return true;
    }

    /**
     * Updates an existing environment.
     *
     * @param oldName the current name of the environment
     * @param environment the updated environment
     * @return true if the environment was updated, false if it wasn't found
     */
    public boolean updateEnvironment(String oldName, Environment environment) {
        if (!environments.containsKey(oldName)) {
            return false;
        }

        // If the name has changed, remove the old entry and add a new one
        if (!oldName.equals(environment.getName())) {
            environments.remove(oldName);
            environments.put(environment.getName(), environment);

            // Update active environment name if necessary
            if (oldName.equals(activeEnvironmentName)) {
                activeEnvironmentName = environment.getName();
            }
        } else {
            // Otherwise, just update the existing entry
            environments.put(oldName, environment);
        }

        return true;
    }

    /**
     * Removes an environment by name.
     *
     * @param name the name of the environment to remove
     * @return true if the environment was removed, false if it wasn't found
     */
    public boolean removeEnvironment(String name) {
        if (!environments.containsKey(name)) {
            return false;
        }

        // Don't remove the active environment
        if (name.equals(activeEnvironmentName)) {
            return false;
        }

        environments.remove(name);
        return true;
    }

    /**
     * Gets the value of a variable from the active environment.
     *
     * @param key the variable key
     * @return an Optional containing the variable value if found, or empty if not found
     */
    public Optional<String> getVariableValue(String key) {
        return getActiveEnvironment().flatMap(env -> env.getVariableValue(key));
    }

    /**
     * Gets the number of environments.
     *
     * @return the number of environments
     */
    public int size() {
        return environments.size();
    }

    /**
     * Clears all environments.
     */
    public void clear() {
        environments.clear();
        activeEnvironmentName = null;
    }

    /**
     * Saves the environment settings to a file.
     */
    public void saveSettings() {
        try {
            File settingsFile = new File(ENV_SETTINGS);
            settingsFile.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(settingsFile);
                    ObjectOutputStream oos = new ObjectOutputStream(out)) {
                oos.writeObject(this);
                oos.flush();
                log.info("Saved environment settings with {} environments", environments.size());
            }
        } catch (Exception e) {
            log.error("Problem serializing environment settings", e);
        }
    }

    /**
     * Loads the environment settings from a file.
     *
     * @return the loaded EnvironmentManager, or a new instance if loading fails
     */
    public static EnvironmentManager loadSettings() {
        Path path = Paths.get(ENV_SETTINGS);
        if (path.toFile().isFile()) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(Files.readAllBytes(path)))) {
                EnvironmentManager manager = (EnvironmentManager) ois.readObject();
                log.info("Loaded environment settings with {} environments", manager.size());
                return manager;
            } catch (Exception e) {
                log.error("Problem deserializing environment settings", e);
                try {
                    Files.delete(path);
                } catch (IOException ex) {
                    log.error("Failed to delete corrupted environment settings file", ex);
                }
            }
        }
        return new EnvironmentManager();
    }

    /**
     * Resolves a string by replacing environment variable references with their values. Environment variables are referenced
     * using the syntax {{variable_name}}.
     *
     * @param input the input string to resolve
     * @return the resolved string with environment variables replaced
     */
    public String resolveVariables(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        int startIndex = 0;
        while ((startIndex = result.indexOf("{{", startIndex)) >= 0) {
            int endIndex = result.indexOf("}}", startIndex);
            if (endIndex < 0) {
                break;
            }

            String variableName = result.substring(startIndex + 2, endIndex).trim();
            Optional<String> variableValue = getVariableValue(variableName);

            if (variableValue.isPresent()) {
                result = result.substring(0, startIndex) + variableValue.get() + result.substring(endIndex + 2);
                // Don't increment startIndex to allow for nested variables
            } else {
                // Variable not found, leave it as is and move past it
                startIndex = endIndex + 2;
            }
        }

        return result;
    }
}