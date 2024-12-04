package com.footballsim.controllers;

import com.footballsim.models.TeamRobot;
import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages robot creation, placement, and team assignment in the football simulator.
 * Provides methods for adding and removing robots while ensuring thread safety.
 */
public class RobotManager {
    private final List<TeamRobot> redTeam;
    private final List<TeamRobot> blueTeam;
    private final double fieldWidth;
    private final double fieldHeight;
    private final double borderWidth;
    
    /**
     * Creates a new robot manager with the specified field dimensions
     * 
     * @param fieldWidth Width of the playing field
     * @param fieldHeight Height of the playing field
     * @param borderWidth Width of the field border
     */
    public RobotManager(double fieldWidth, double fieldHeight, double borderWidth) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.borderWidth = borderWidth;
        this.redTeam = new CopyOnWriteArrayList<>();
        this.blueTeam = new CopyOnWriteArrayList<>();
    }
    
    /**
     * Adds a new robot to the specified team
     * 
     * @param isRedTeam True to add to red team, false for blue team
     * @param role Role to assign to the robot
     * @return The created robot
     */
    public TeamRobot addRobot(boolean isRedTeam, TeamRobot.RobotRole role) {
        // Calculate default position based on team
        double x = isRedTeam ? 
            borderWidth + fieldWidth * 0.25 : // Red team starts on left side
            borderWidth + fieldWidth * 0.75;  // Blue team starts on right side
        double y = borderWidth + fieldHeight / 2;
        
        // Create new robot
        TeamRobot robot = new TeamRobot(x, y, isRedTeam);
        robot.setRole(role);
        
        // Add to appropriate team
        if (isRedTeam) {
            redTeam.add(robot);
        } else {
            blueTeam.add(robot);
        }
        
        return robot;
    }
    
    /**
     * Removes a robot from its team
     * 
     * @param robot Robot to remove
     */
    public void removeRobot(TeamRobot robot) {
        if (robot.isRedTeam()) {
            redTeam.remove(robot);
        } else {
            blueTeam.remove(robot);
        }
    }
    
    /**
     * Gets all robots on a specific team
     * 
     * @param isRedTeam True for red team, false for blue team
     * @return List of robots on the specified team
     */
    public List<TeamRobot> getTeamRobots(boolean isRedTeam) {
        return isRedTeam ? new ArrayList<>(redTeam) : new ArrayList<>(blueTeam);
    }
    
    /**
     * Renders all robots on the canvas
     * 
     * @param gc Graphics context to draw on
     */
    public void drawRobots(GraphicsContext gc) {
        for (TeamRobot robot : redTeam) {
            robot.draw(gc);
        }
        for (TeamRobot robot : blueTeam) {
            robot.draw(gc);
        }
    }
    
    /**
     * Updates all robots' positions and states
     */
    public void updateRobots() {
        for (TeamRobot robot : redTeam) {
            robot.update();
        }
        for (TeamRobot robot : blueTeam) {
            robot.update();
        }
    }
    
    /**
     * Clears all robots from both teams
     */
    public void clearRobots() {
        redTeam.clear();
        blueTeam.clear();
    }
}