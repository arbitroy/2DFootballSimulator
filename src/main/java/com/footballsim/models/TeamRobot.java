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

        // Set initial rotation based on team
        this.rotation = isRedTeam ? 0 : 180; // Red team faces right, blue team faces left
    }

    /**
     * Updates the robot's position and behavior
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

        // Update position based on movement
        super.update();
    }

    private void updateGoalkeeper(Ball ball) {
        // Stay on goal line but move up/down based on ball position
        double targetY = ball.getY();
        double currentY = y + height/2;

        // Only move vertically
        if (Math.abs(currentY - targetY) > 5) {
            // Move towards ball's Y position
            rotation = (currentY > targetY) ? 270 : 90;
            accelerate();
        } else {
            brake();
        }
    }


    private void updateDefender(Ball ball, List<TeamRobot> opponents) {
        double ballX = ball.getX();
        double ballY = ball.getY();

        // Calculate whether ball is in our half
        boolean ballInOurHalf = isRedTeam ?
                ballX < x : // For red team, our half is left side
                ballX > x;  // For blue team, our half is right side

        if (ballInOurHalf) {
            // Move to intercept ball
            double angleToTarget = calculateAngleToBall(ball);
            double angleDiff = normalizeAngle(angleToTarget - rotation);

            // Turn towards ball
            if (Math.abs(angleDiff) > 10) {
                turn(angleDiff > 0 ? turnRate : -turnRate);
            }

            // Move towards ball if facing it
            if (Math.abs(angleDiff) < 45) {
                accelerate();
            } else {
                brake();
            }
        } else {
            // Return to defensive position
            moveToDefensivePosition();
        }
    }

    private void updateAttacker(Ball ball, List<AbstractArenaObject> obstacles) {
        double ballX = ball.getX();
        double ballY = ball.getY();

        // Calculate angle to ball
        double angleToTarget = calculateAngleToBall(ball);
        double angleDiff = normalizeAngle(angleToTarget - rotation);

        // Check if path to ball is clear
        boolean pathClear = true;
        for (AbstractArenaObject obstacle : obstacles) {
            if (detectObstacle(obstacle)) {
                pathClear = false;
                break;
            }
        }

        if (pathClear) {
            // Move towards ball
            if (Math.abs(angleDiff) > 10) {
                turn(angleDiff > 0 ? turnRate : -turnRate);
            }

            // Only move forward if roughly facing the ball
            if (Math.abs(angleDiff) < 45) {
                accelerate();
            } else {
                brake();
            }
        } else {
            // Obstacle avoidance
            // Turn away from detected obstacles
            turn(45);
            brake();
        }
    }

    /**
     * Calculates angle to ball
     */
    private double calculateAngleToBall(Ball ball) {
        double dx = ball.getX() - (x + width/2);
        double dy = ball.getY() - (y + height/2);
        return Math.toDegrees(Math.atan2(dy, dx));
    }

    /**
     * Normalizes angle to -180 to 180 range
     */
    private double normalizeAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Moves robot to defensive position
     */
    private void moveToDefensivePosition() {
        // Calculate default defensive position based on team
        double targetX = isRedTeam ?
                x - 50 : // Red team moves left
                x + 50;  // Blue team moves right

        double angleToPosition = Math.toDegrees(Math.atan2(0, targetX - x));
        double angleDiff = normalizeAngle(angleToPosition - rotation);

        if (Math.abs(angleDiff) > 10) {
            turn(angleDiff > 0 ? turnRate : -turnRate);
        }

        if (Math.abs(angleDiff) < 45) {
            accelerate();
        } else {
            brake();
        }
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

    // Getters and setters
    public boolean isRedTeam() { return isRedTeam; }
    
    public RobotRole getCurrentRole() { return currentRole; }
    
    public void setRole(RobotRole role) { this.currentRole = role; }
}