package com.footballsim.views;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.footballsim.models.TeamRobot;

public class RobotControlPanel extends VBox {
    private ToggleGroup teamToggle;
    private Spinner<Double> speedSpinner;
    private Spinner<Double> sensorRangeSpinner;
    private ListView<String> robotList;

    public RobotControlPanel() {
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");

        // Team selection
        Label teamLabel = new Label("Select Team:");
        teamToggle = new ToggleGroup();
        RadioButton redTeam = new RadioButton("Red Team");
        RadioButton blueTeam = new RadioButton("Blue Team");
        redTeam.setToggleGroup(teamToggle);
        blueTeam.setToggleGroup(teamToggle);
        redTeam.setSelected(true);

        // Robot properties
        Label speedLabel = new Label("Robot Speed:");
        speedSpinner = new Spinner<>(0.1, 5.0, 1.0, 0.1);
        speedSpinner.setEditable(true);

        Label sensorLabel = new Label("Sensor Range:");
        sensorRangeSpinner = new Spinner<>(10.0, 100.0, 50.0, 5.0);
        sensorRangeSpinner.setEditable(true);

        // Robot list
        Label robotsLabel = new Label("Active Robots:");
        robotList = new ListView<>();
        robotList.setPrefHeight(200);

        Button addButton = new Button("Add Robot");
        Button removeButton = new Button("Remove Selected");

        addButton.setOnAction(e -> addRobot());
        removeButton.setOnAction(e -> removeSelectedRobot());

        getChildren().addAll(
            teamLabel, redTeam, blueTeam,
            new Separator(),
            speedLabel, speedSpinner,
            sensorLabel, sensorRangeSpinner,
            new Separator(),
            robotsLabel, robotList,
            addButton, removeButton
        );
    }

    public void addRobot(boolean isRedTeam) {
        // Create new robot with current settings
        TeamRobot robot = new TeamRobot(
            300, // Default x position
            200, // Default y position
            isRedTeam
        );
        robot.setRole(TeamRobot.RobotRole.ATTACKER); // Default role
        
        // Add to list
        String robotName = (isRedTeam ? "Red" : "Blue") + " Robot " + (robotList.getItems().size() + 1);
        robotList.getItems().add(robotName);
    }

    private void addRobot() {
        // Get current team selection
        RadioButton selectedTeam = (RadioButton) teamToggle.getSelectedToggle();
        boolean isRedTeam = selectedTeam.getText().equals("Red Team");
        
        // Add robot with appropriate team
        addRobot(isRedTeam);
    }

    private void removeSelectedRobot() {
        int selectedIndex = robotList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            robotList.getItems().remove(selectedIndex);
        }
    }
}