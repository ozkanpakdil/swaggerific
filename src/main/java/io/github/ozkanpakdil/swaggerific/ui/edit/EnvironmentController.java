package io.github.ozkanpakdil.swaggerific.ui.edit;

import ch.qos.logback.classic.Logger;
import io.github.ozkanpakdil.swaggerific.data.Environment;
import io.github.ozkanpakdil.swaggerific.data.EnvironmentManager;
import io.github.ozkanpakdil.swaggerific.data.EnvironmentVariable;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the environment management UI.
 */
public class EnvironmentController implements Initializable {
    private static final Logger log = (Logger) LoggerFactory.getLogger(EnvironmentController.class);

    @FXML
    private ComboBox<Environment> environmentSelector;

    @FXML
    private TextField txtEnvironmentName;

    @FXML
    private TextField txtEnvironmentDescription;

    @FXML
    private TableView<EnvironmentVariable> tableVariables;

    @FXML
    private TableColumn<EnvironmentVariable, String> colVariableKey;

    @FXML
    private TableColumn<EnvironmentVariable, String> colVariableValue;

    @FXML
    private TableColumn<EnvironmentVariable, Boolean> colVariableSecret;

    @FXML
    private TextField txtVariableKey;

    @FXML
    private TextField txtVariableValue;

    @FXML
    private CheckBox chkVariableSecret;

    @FXML
    private Button btnSaveEnvironment;

    @FXML
    private Button btnCancelEnvironment;

    @FXML
    private Button btnSaveVariable;

    @FXML
    private Button btnCancelVariable;

    @FXML
    private Button btnClose;
    
    @FXML
    private Button btnNewEnvironment;
    
    @FXML
    private Button btnEditEnvironment;
    
    @FXML
    private Button btnDeleteEnvironment;
    
    @FXML
    private Button btnAddVariable;
    
    @FXML
    private Button btnEditVariable;
    
    @FXML
    private Button btnDeleteVariable;

    private MainController mainController;
    private EnvironmentManager environmentManager;
    private ObservableList<EnvironmentVariable> variablesList;
    private Environment currentEnvironment;
    private EnvironmentVariable currentVariable;
    private boolean isEditingEnvironment = false;
    private boolean isEditingVariable = false;
    private String originalEnvironmentName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize the environment manager
        environmentManager = EnvironmentManager.loadSettings();

        // Set up the environment selector
        refreshEnvironmentSelector();

        // Set up the variables table
        setupVariablesTable();

        // Set up initial UI state
        setEnvironmentEditMode(false);
        setVariableEditMode(false);
        updateVariablesList();

        // Add listener to environment selector
        environmentSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                environmentManager.setActiveEnvironment(newVal.getName());
                currentEnvironment = newVal;
                updateVariablesList();
            }
        });

        // Select the active environment
        environmentManager.getActiveEnvironment().ifPresent(env -> {
            environmentSelector.getSelectionModel().select(env);
            currentEnvironment = env;
        });
    }

    /**
     * Sets the main controller reference.
     *
     * @param mainController the main controller
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Sets up the variables table with columns and cell factories.
     */
    private void setupVariablesTable() {
        variablesList = FXCollections.observableArrayList();
        tableVariables.setItems(variablesList);

        // Set up the key column
        colVariableKey.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getKey()));

        // Set up the value column to mask secret values
        colVariableValue.setCellValueFactory(cellData -> {
            EnvironmentVariable variable = cellData.getValue();
            String displayValue = variable.isSecret() ? "********" : variable.getValue();
            return new SimpleStringProperty(displayValue);
        });

        // Set up the secret column
        colVariableSecret.setCellValueFactory(cellData -> 
            new SimpleBooleanProperty(cellData.getValue().isSecret()));
        colVariableSecret.setCellFactory(CheckBoxTableCell.forTableColumn(colVariableSecret));

        // Add listener for variable selection
        tableVariables.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isEditingVariable) {
                currentVariable = newVal;
                txtVariableKey.setText(newVal.getKey());
                txtVariableValue.setText(newVal.getValue());
                chkVariableSecret.setSelected(newVal.isSecret());
            }
        });
    }

    /**
     * Refreshes the environment selector with the current list of environments.
     */
    private void refreshEnvironmentSelector() {
        ObservableList<Environment> environments = FXCollections.observableArrayList(
                environmentManager.getAllEnvironments());
        environmentSelector.setItems(environments);
    }

    /**
     * Updates the variables list with the variables from the current environment.
     */
    private void updateVariablesList() {
        variablesList.clear();
        if (currentEnvironment != null) {
            variablesList.addAll(currentEnvironment.getAllVariables());
        }
    }

    /**
     * Sets the environment edit mode.
     *
     * @param editing true to enable editing, false to disable
     */
    private void setEnvironmentEditMode(boolean editing) {
        isEditingEnvironment = editing;
        txtEnvironmentName.setDisable(!editing);
        txtEnvironmentDescription.setDisable(!editing);
        btnSaveEnvironment.setDisable(!editing);
        btnCancelEnvironment.setDisable(!editing);
        environmentSelector.setDisable(editing);
        btnNewEnvironment.setDisable(editing);
        btnEditEnvironment.setDisable(editing);
        btnDeleteEnvironment.setDisable(editing);

        if (!editing) {
            txtEnvironmentName.clear();
            txtEnvironmentDescription.clear();
        }
    }

    /**
     * Sets the variable edit mode.
     *
     * @param editing true to enable editing, false to disable
     */
    private void setVariableEditMode(boolean editing) {
        isEditingVariable = editing;
        txtVariableKey.setDisable(!editing);
        txtVariableValue.setDisable(!editing);
        chkVariableSecret.setDisable(!editing);
        btnSaveVariable.setDisable(!editing);
        btnCancelVariable.setDisable(!editing);
        btnAddVariable.setDisable(editing);
        btnEditVariable.setDisable(editing);
        btnDeleteVariable.setDisable(editing);
        tableVariables.setDisable(editing);

        if (!editing) {
            txtVariableKey.clear();
            txtVariableValue.clear();
            chkVariableSecret.setSelected(false);
        }
    }

    /**
     * Handles the "New Environment" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleNewEnvironment(ActionEvent event) {
        setEnvironmentEditMode(true);
        originalEnvironmentName = null;
    }

    /**
     * Handles the "Edit Environment" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleEditEnvironment(ActionEvent event) {
        Environment selectedEnvironment = environmentSelector.getSelectionModel().getSelectedItem();
        if (selectedEnvironment != null) {
            txtEnvironmentName.setText(selectedEnvironment.getName());
            txtEnvironmentDescription.setText(selectedEnvironment.getDescription());
            originalEnvironmentName = selectedEnvironment.getName();
            setEnvironmentEditMode(true);
        } else {
            showAlert(Alert.AlertType.WARNING, "No Environment Selected", 
                    "Please select an environment to edit.");
        }
    }

    /**
     * Handles the "Delete Environment" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleDeleteEnvironment(ActionEvent event) {
        Environment selectedEnvironment = environmentSelector.getSelectionModel().getSelectedItem();
        if (selectedEnvironment != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Environment");
            alert.setHeaderText("Delete Environment: " + selectedEnvironment.getName());
            alert.setContentText("Are you sure you want to delete this environment? This action cannot be undone.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (environmentManager.removeEnvironment(selectedEnvironment.getName())) {
                    refreshEnvironmentSelector();
                    environmentManager.saveSettings();
                    
                    // Select another environment if available
                    if (!environmentManager.getAllEnvironments().isEmpty()) {
                        environmentSelector.getSelectionModel().selectFirst();
                    } else {
                        currentEnvironment = null;
                        updateVariablesList();
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Cannot Delete Environment", 
                            "Cannot delete the active environment. Please select another environment as active first.");
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "No Environment Selected", 
                    "Please select an environment to delete.");
        }
    }

    /**
     * Handles the "Save Environment" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleSaveEnvironment(ActionEvent event) {
        String name = txtEnvironmentName.getText().trim();
        String description = txtEnvironmentDescription.getText().trim();

        if (name.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Invalid Environment Name", 
                    "Environment name cannot be empty.");
            return;
        }

        // Check if the name already exists (for new environments or renamed environments)
        if (originalEnvironmentName == null || !originalEnvironmentName.equals(name)) {
            Optional<Environment> existingEnv = environmentManager.getEnvironment(name);
            if (existingEnv.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Duplicate Environment Name", 
                        "An environment with this name already exists.");
                return;
            }
        }

        if (originalEnvironmentName == null) {
            // Creating a new environment
            Environment newEnvironment = new Environment(name, description);
            environmentManager.addEnvironment(newEnvironment);
            refreshEnvironmentSelector();
            environmentSelector.getSelectionModel().select(newEnvironment);
            currentEnvironment = newEnvironment;
        } else {
            // Updating an existing environment
            Environment updatedEnvironment = new Environment(name, description);
            environmentManager.updateEnvironment(originalEnvironmentName, updatedEnvironment);
            refreshEnvironmentSelector();
            environmentSelector.getSelectionModel().select(updatedEnvironment);
            currentEnvironment = updatedEnvironment;
        }

        environmentManager.saveSettings();
        setEnvironmentEditMode(false);
    }

    /**
     * Handles the "Cancel Environment" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleCancelEnvironment(ActionEvent event) {
        setEnvironmentEditMode(false);
    }

    /**
     * Handles the "Add Variable" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleAddVariable(ActionEvent event) {
        if (currentEnvironment == null) {
            showAlert(Alert.AlertType.WARNING, "No Environment Selected", 
                    "Please select an environment first.");
            return;
        }

        currentVariable = null;
        setVariableEditMode(true);
    }

    /**
     * Handles the "Edit Variable" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleEditVariable(ActionEvent event) {
        EnvironmentVariable selectedVariable = tableVariables.getSelectionModel().getSelectedItem();
        if (selectedVariable != null) {
            currentVariable = selectedVariable;
            txtVariableKey.setText(selectedVariable.getKey());
            txtVariableValue.setText(selectedVariable.getValue());
            chkVariableSecret.setSelected(selectedVariable.isSecret());
            setVariableEditMode(true);
        } else {
            showAlert(Alert.AlertType.WARNING, "No Variable Selected", 
                    "Please select a variable to edit.");
        }
    }

    /**
     * Handles the "Delete Variable" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleDeleteVariable(ActionEvent event) {
        if (currentEnvironment == null) {
            return;
        }

        EnvironmentVariable selectedVariable = tableVariables.getSelectionModel().getSelectedItem();
        if (selectedVariable != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Variable");
            alert.setHeaderText("Delete Variable: " + selectedVariable.getKey());
            alert.setContentText("Are you sure you want to delete this variable? This action cannot be undone.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                currentEnvironment.removeVariable(selectedVariable.getKey());
                updateVariablesList();
                environmentManager.saveSettings();
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "No Variable Selected", 
                    "Please select a variable to delete.");
        }
    }

    /**
     * Handles the "Save Variable" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleSaveVariable(ActionEvent event) {
        if (currentEnvironment == null) {
            return;
        }

        String key = txtVariableKey.getText().trim();
        String value = txtVariableValue.getText();
        boolean isSecret = chkVariableSecret.isSelected();

        if (key.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Invalid Variable Key", 
                    "Variable key cannot be empty.");
            return;
        }

        // If editing an existing variable with a different key, remove the old one
        if (currentVariable != null && !currentVariable.getKey().equals(key)) {
            currentEnvironment.removeVariable(currentVariable.getKey());
        }

        currentEnvironment.setVariable(key, value, isSecret);
        updateVariablesList();
        environmentManager.saveSettings();
        setVariableEditMode(false);
    }

    /**
     * Handles the "Cancel Variable" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleCancelVariable(ActionEvent event) {
        setVariableEditMode(false);
    }

    /**
     * Handles the "Close" button click.
     *
     * @param event the action event
     */
    @FXML
    private void handleClose(ActionEvent event) {
        environmentManager.saveSettings();
        ((Stage) btnClose.getScene().getWindow()).close();
    }

    /**
     * Shows an alert dialog.
     *
     * @param type    the alert type
     * @param title   the alert title
     * @param content the alert content
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}