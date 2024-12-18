package com.footballsim.views;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

import com.footballsim.models.Obstacle;

/**
 * Panel for controlling obstacle creation and configuration
 */
public class ObstacleControlPanel extends VBox {
    private ComboBox<Obstacle.ObstacleType> obstacleTypeComboBox;
    private ColorPicker colorPicker;
    private Spinner<Double> widthSpinner;
    private Spinner<Double> heightSpinner;
    private ListView<String> obstacleList;
    private Button addButton;
    private Button removeButton;

    // Callback interfaces
    private ObstacleAddCallback onObstacleAdd;
    private ObstacleRemoveCallback onObstacleRemove;

    public interface ObstacleAddCallback {
        boolean onAdd(Obstacle.ObstacleType type, double width, double height, Color color);
    }

    public interface ObstacleRemoveCallback {
        boolean onRemove(int index);
    }

    /**
     * Creates a new obstacle control panel
     */
    public ObstacleControlPanel() {
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");

        setupTypeSelection();
        setupDimensionControls();
        setupColorPicker();
        setupObstacleList();
        setupButtons();

        VBox.setVgrow(obstacleList, Priority.ALWAYS);
    }

    /**
     * Sets up obstacle type selection control
     */
    private void setupTypeSelection() {
        Label typeLabel = new Label("Obstacle Type:");
        obstacleTypeComboBox = new ComboBox<>();
        obstacleTypeComboBox.getItems().addAll(Obstacle.ObstacleType.values());
        obstacleTypeComboBox.setValue(Obstacle.ObstacleType.WALL);
        obstacleTypeComboBox.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(typeLabel, obstacleTypeComboBox);
    }

    /**
     * Sets up dimension control spinners
     */
    private void setupDimensionControls() {
        Label widthLabel = new Label("Width:");
        widthSpinner = new Spinner<>(20.0, 100.0, 40.0, 5.0);
        widthSpinner.setEditable(true);
        widthSpinner.setPrefWidth(120);

        Label heightLabel = new Label("Height:");
        heightSpinner = new Spinner<>(20.0, 100.0, 40.0, 5.0);
        heightSpinner.setEditable(true);
        heightSpinner.setPrefWidth(120);

        GridPane dimensionGrid = new GridPane();
        dimensionGrid.setHgap(10);
        dimensionGrid.setVgap(5);
        dimensionGrid.add(widthLabel, 0, 0);
        dimensionGrid.add(widthSpinner, 1, 0);
        dimensionGrid.add(heightLabel, 0, 1);
        dimensionGrid.add(heightSpinner, 1, 1);

        getChildren().add(dimensionGrid);
    }

    /**
     * Sets up color selection control
     */
    private void setupColorPicker() {
        Label colorLabel = new Label("Color:");
        colorPicker = new ColorPicker(Color.GRAY);
        colorPicker.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(colorLabel, colorPicker);
    }

    /**
     * Sets up obstacle list view
     */
    private void setupObstacleList() {
        Label obstaclesLabel = new Label("Placed Obstacles:");
        obstacleList = new ListView<>();
        obstacleList.setPrefHeight(200);

        getChildren().addAll(obstaclesLabel, obstacleList);
    }

    /**
     * Sets up control buttons
     */
    private void setupButtons() {
        addButton = new Button("Add Obstacle");
        removeButton = new Button("Remove Selected");

        addButton.setMaxWidth(Double.MAX_VALUE);
        removeButton.setMaxWidth(Double.MAX_VALUE);

        addButton.setOnAction(e -> addObstacle());
        removeButton.setOnAction(e -> removeSelectedObstacle());

        getChildren().addAll(new Separator(), addButton, removeButton);
    }

    /**
     * Sets callback for obstacle addition
     * @param callback Callback to execute when adding obstacle
     */
    public void setOnObstacleAdd(ObstacleAddCallback callback) {
        this.onObstacleAdd = callback;
    }

    /**
     * Sets callback for obstacle removal
     * @param callback Callback to execute when removing obstacle
     */
    public void setOnObstacleRemove(ObstacleRemoveCallback callback) {
        this.onObstacleRemove = callback;
    }

    /**
     * Handles adding a new obstacle
     */
    void addObstacle() {
        if (onObstacleAdd != null) {
            boolean success = onObstacleAdd.onAdd(
                obstacleTypeComboBox.getValue(),
                widthSpinner.getValue(),
                heightSpinner.getValue(),
                colorPicker.getValue()
            );
            
            if (!success) {
                showErrorAlert("Invalid Placement", 
                    "Could not place obstacle. Please ensure it doesn't overlap with other objects or goals.");
            }
        }
    }

    /**
     * Handles removing the selected obstacle
     */
    private void removeSelectedObstacle() {
        int selectedIndex = obstacleList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0 && onObstacleRemove != null) {
            onObstacleRemove.onRemove(selectedIndex);
        }
    }

    /**
     * Updates the obstacle list with new names
     * @param names List of obstacle names to display
     */
    public void updateObstacleList(List<String> names) {
        obstacleList.getItems().clear();
        obstacleList.getItems().addAll(names);
    }

    /**
     * Shows error alert dialog
     */
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Enables or disables the panel controls
     * @param disabled true to disable controls
     */
    public void setControlsDisabled(boolean disabled) {
        obstacleTypeComboBox.setDisable(disabled);
        widthSpinner.setDisable(disabled);
        heightSpinner.setDisable(disabled);
        colorPicker.setDisable(disabled);
        addButton.setDisable(disabled);
        removeButton.setDisable(disabled);
    }
}