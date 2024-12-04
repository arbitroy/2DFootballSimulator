package com.footballsim.views;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.footballsim.models.TeamRobot;

/**
 * Panel for controlling robot creation and configuration
 */
public class RobotControlPanel extends VBox {
    private ToggleGroup teamToggle;
    private ComboBox<TeamRobot.RobotRole> roleComboBox;
    private ListView<String> robotList;

    /**
     * Creates a new robot control panel
     */
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

        // Role selection
        Label roleLabel = new Label("Robot Role:");
        roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(TeamRobot.RobotRole.values());
        roleComboBox.setValue(TeamRobot.RobotRole.ATTACKER);

        // Robot list
        Label robotsLabel = new Label("Active Robots:");
        robotList = new ListView<>();
        robotList.setPrefHeight(200);

        getChildren().addAll(
            teamLabel, redTeam, blueTeam,
            new Separator(),
            roleLabel, roleComboBox,
            new Separator(),
            robotsLabel, robotList
        );
    }

    /**
     * Gets whether red team is selected
     * @return true if red team is selected
     */
    public boolean isRedTeamSelected() {
        RadioButton selectedTeam = (RadioButton) teamToggle.getSelectedToggle();
        return selectedTeam.getText().equals("Red Team");
    }

    /**
     * Gets the currently selected robot role
     * @return Selected robot role
     */
    public TeamRobot.RobotRole getSelectedRole() {
        return roleComboBox.getValue();
    }

    /**
     * Updates the robot list display
     * @param redTeamCount Number of red team robots
     * @param blueTeamCount Number of blue team robots
     */
    public void updateRobotCounts(int redTeamCount, int blueTeamCount) {
        robotList.getItems().clear();
        for (int i = 0; i < redTeamCount; i++) {
            robotList.getItems().add("Red Team Robot " + (i + 1));
        }
        for (int i = 0; i < blueTeamCount; i++) {
            robotList.getItems().add("Blue Team Robot " + (i + 1));
        }
    }

    /**
     * Gets the index of the selected robot in the list
     * @return Selected index or -1 if none selected
     */
    public int getSelectedRobotIndex() {
        return robotList.getSelectionModel().getSelectedIndex();
    }
}