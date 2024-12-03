package com.footballsim.views;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.footballsim.models.Obstacle;

public class ObstacleControlPanel extends VBox {
    private ComboBox<String> obstacleTypeComboBox;
    private Spinner<Double> sizeSpinner;
    private ListView<String> obstacleList;

    public ObstacleControlPanel() {
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");

        // Obstacle type selection
        Label typeLabel = new Label("Obstacle Type:");
        obstacleTypeComboBox = new ComboBox<>();
        obstacleTypeComboBox.getItems().addAll("Wall", "Circle", "Rectangle");
        obstacleTypeComboBox.setValue("Wall");

        // Size control
        Label sizeLabel = new Label("Size:");
        sizeSpinner = new Spinner<>(10.0, 100.0, 30.0, 5.0);
        sizeSpinner.setEditable(true);

        // Obstacle list
        Label obstaclesLabel = new Label("Placed Obstacles:");
        obstacleList = new ListView<>();
        obstacleList.setPrefHeight(200);

        Button addButton = new Button("Add Obstacle");
        Button removeButton = new Button("Remove Selected");

        addButton.setOnAction(e -> addObstacle());
        removeButton.setOnAction(e -> removeSelectedObstacle());

        getChildren().addAll(
            typeLabel, obstacleTypeComboBox,
            sizeLabel, sizeSpinner,
            new Separator(),
            obstaclesLabel, obstacleList,
            addButton, removeButton
        );
    }

    public void addObstacle() {
        // Create new obstacle with current settings
        double size = sizeSpinner.getValue();
        Obstacle.ObstacleType type;
        switch (obstacleTypeComboBox.getValue()) {
            case "Circle": type = Obstacle.ObstacleType.CIRCLE; break;
            case "Rectangle": type = Obstacle.ObstacleType.RECTANGLE; break;
            default: type = Obstacle.ObstacleType.WALL; break;
        }
        
        // Add to list
        String obstacleName = type.toString() + " " + (obstacleList.getItems().size() + 1);
        obstacleList.getItems().add(obstacleName);
    }

    private void removeSelectedObstacle() {
        int selectedIndex = obstacleList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            obstacleList.getItems().remove(selectedIndex);
        }
    }
}