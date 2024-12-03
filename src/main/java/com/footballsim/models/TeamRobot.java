package com.footballsim.models;

import java.util.List;

import com.footballsim.utils.Line;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a team-specific robot with additional team behaviors
 */
public class TeamRobot extends AbstractRobot {
    private boolean isRedTeam;
    private RobotRole currentRole;
    private static final double DIRECTION_INDICATOR_LENGTH = 15.0;

    public enum RobotRole {
        GOALKEEPER,
        DEFENDER,
        ATTACKER
    }

    /**
     * Creates a new team robot
     * @param x Starting x coordinate
     * @param y Starting y coordinate
     * @param isRedTeam true if on red team, false for blue team
     */
    public TeamRobot(double x, double y, boolean isRedTeam) {
        super(x, y, 3.0, 100.0); // Standard speed and sensor range
        this.isRedTeam = isRedTeam;
        this.currentRole = RobotRole.ATTACKER; // Default role
    }

    /**
     * Updates the robot's position and behavior
     * @param ball The game ball
     * @param obstacles List of obstacles in the arena
     */
    @Override
    public void update() {
        super.update();
        // Additional team-specific update logic could go here
    }

    /**
     * Draws the robot on the canvas
     * @param gc Graphics context to draw on
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.save(); // Save current graphics state
        
        // Move to robot's position and apply rotation
        gc.translate(x + width/2, y + height/2);
        gc.rotate(rotation);

        // Draw robot body
        gc.setFill(isRedTeam ? Color.RED : Color.BLUE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.fillRect(-width/2, -height/2, width, height);
        gc.strokeRect(-width/2, -height/2, width, height);

        // Draw direction indicator
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(2);
        gc.strokeLine(0, 0, DIRECTION_INDICATOR_LENGTH, 0);

        // Draw role indicator
        gc.setFill(getRoleColor());
        gc.fillOval(-width/4, -height/4, width/2, height/2);

        // Optionally draw sensor lines for debugging
        if (false) { // Set to true to see sensor lines
            drawSensorLines(gc);
        }

        gc.restore(); // Restore graphics state
    }

    /**
     * Gets the color associated with the current role
     * @return Color for the current role
     */
    private Color getRoleColor() {
        switch (currentRole) {
            case GOALKEEPER: return Color.WHITE;
            case DEFENDER: return Color.YELLOW;
            case ATTACKER: return Color.GREEN;
            default: return Color.GRAY;
        }
    }

    /**
     * Draws sensor lines for debugging
     * @param gc Graphics context to draw on
     */
    private void drawSensorLines(GraphicsContext gc) {
        Line[] sensorBeams = createSensorBeams();
        gc.setStroke(Color.LIGHTGREEN);
        gc.setLineWidth(0.5);
        
        for (Line beam : sensorBeams) {
            gc.strokeLine(beam.getXY()[0] - x - width/2, 
                         beam.getXY()[1] - y - height/2,
                         beam.getXY()[2] - x - width/2, 
                         beam.getXY()[3] - y - height/2);
        }
    }

    /**
     * Updates the robot's behavior based on game state
     * @param ball The game ball
     * @param obstacles List of obstacles
     * @param teammates List of teammate robots
     * @param opponents List of opponent robots
     */
    public void updateBehavior(Ball ball, List<AbstractArenaObject> obstacles,
            List<TeamRobot> teammates, List<TeamRobot> opponents) {
        switch (currentRole) {
            case GOALKEEPER:
                updateGoalkeeper(ball);
                break;
            case DEFENDER:
                updateDefender(ball, opponents);
                break;
            case ATTACKER:
                updateAttacker(ball, obstacles);
                break;
        }
    }

    private void updateGoalkeeper(Ball ball) {
        double targetY = ball.getY();
        if (Math.abs(y - targetY) > 5) {
            if (y < targetY)
                accelerate();
            else
                brake();
        }
    }

    private void updateDefender(Ball ball, List<TeamRobot> opponents) {
        TeamRobot nearestOpponent = findNearestOpponent(opponents);
        if (nearestOpponent != null) {
            // Calculate intercept position
            double interceptX = (nearestOpponent.getX() + ball.getX()) / 2;
            double interceptY = (nearestOpponent.getY() + ball.getY()) / 2;
            
            // Move to intercept position
            double targetAngle = Math.toDegrees(Math.atan2(
                interceptY - y, interceptX - x));
            turn(targetAngle - rotation);
            
            if (!detectObstacle(nearestOpponent)) {
                accelerate();
            } else {
                brake();
            }
        }
    }

    private void updateAttacker(Ball ball, List<AbstractArenaObject> obstacles) {
        double targetAngle = calculateAngleToBall(ball);
        turn(targetAngle - rotation);

        boolean obstacleAhead = false;
        for (AbstractArenaObject obstacle : obstacles) {
            if (detectObstacle(obstacle)) {
                obstacleAhead = true;
                break;
            }
        }

        if (!obstacleAhead)
            accelerate();
        else
            brake();
    }

    private TeamRobot findNearestOpponent(List<TeamRobot> opponents) {
        TeamRobot nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (TeamRobot opponent : opponents) {
            double distance = distanceTo(opponent);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = opponent;
            }
        }

        return nearest;
    }

    private double calculateAngleToBall(Ball ball) {
        double dx = ball.getX() - x;
        double dy = ball.getY() - y;
        return Math.toDegrees(Math.atan2(dy, dx));
    }

    // Getters and setters
    public boolean isRedTeam() { return isRedTeam; }
    
    public RobotRole getCurrentRole() { return currentRole; }
    
    public void setRole(RobotRole role) { this.currentRole = role; }
}