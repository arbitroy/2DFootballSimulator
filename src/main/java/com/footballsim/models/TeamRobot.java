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

    private static final double FIELD_MARGIN = 30.0; // Minimum distance from boundaries
    private static final double ROBOT_SPACING = 40.0; // Minimum distance between robots
    private static final double APPROACH_SLOWDOWN = 0.7; // Speed reduction when near targets
    private static final double AVOIDANCE_THRESHOLD = 50.0; // Distance to start avoiding obstacles

    // Home position for formation
    private double homeX;
    private double homeY;


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


    /**
     * Updates the robot's behavior based on game state with improved positioning and collision avoidance
     * @param ball The game ball
     * @param obstacles List of obstacles
     * @param teammates List of teammate robots
     * @param opponents List of opponent robots
     */
    @Override
    public void updateBehavior(Ball ball, List<AbstractArenaObject> obstacles,
                               List<TeamRobot> teammates, List<TeamRobot> opponents) {
        // Get field boundaries
        double fieldStartX = 20; // borderWidth
        double fieldStartY = 20;
        double fieldEndX = fieldStartX + 600; // fieldWidth
        double fieldEndY = fieldStartY + 400;

        // Calculate safe position within boundaries
        double safeX = Math.min(Math.max(x, fieldStartX + FIELD_MARGIN),
                fieldEndX - FIELD_MARGIN - width);
        double safeY = Math.min(Math.max(y, fieldStartY + FIELD_MARGIN),
                fieldEndY - FIELD_MARGIN - height);

        // Update position if too close to boundaries
        if (x != safeX || y != safeY) {
            setPosition(safeX, safeY);
            brake(); // Slow down when near boundaries
        }

        // Role-specific behavior
        switch (currentRole) {
            case GOALKEEPER:
                updateGoalkeeper(ball, fieldStartY, fieldEndY);
                break;
            case DEFENDER:
                updateDefender(ball, teammates, fieldStartX, fieldEndX);
                break;
            case ATTACKER:
                updateAttacker(ball, obstacles, teammates);
                break;
        }

        // Collision avoidance with other robots
        avoidCollisions(teammates, opponents);
    }

    /**
     * Updates goalkeeper behavior with improved positioning
     */
    private void updateGoalkeeper(Ball ball, double fieldStartY, double fieldEndY) {
        double goalY = ball.getY();
        double currentY = y + height/2;
        double goalLineX = isRedTeam ? 40 : 580; // Stay on goal line

        // Limit goalkeeper vertical movement
        double minY = fieldStartY + FIELD_MARGIN;
        double maxY = fieldEndY - FIELD_MARGIN - height;
        goalY = Math.min(Math.max(goalY, minY), maxY);

        // Move to goal line position
        x = goalLineX;

        // Smooth vertical movement
        if (Math.abs(currentY - goalY) > 5) {
            double moveSpeed = Math.min(maxSpeed * 0.5, Math.abs(currentY - goalY) * 0.1);
            y += (goalY > currentY ? moveSpeed : -moveSpeed);
        }
    }

    /**
     * Updates defender behavior with improved positioning and spacing
     */
    private void updateDefender(Ball ball, List<TeamRobot> teammates,
                                double fieldStartX, double fieldEndX) {
        double defenseLineX = isRedTeam ?
                fieldStartX + (fieldEndX - fieldStartX) * 0.3 :
                fieldStartX + (fieldEndX - fieldStartX) * 0.7;

        // Maintain defensive formation
        double targetX = defenseLineX;
        double targetY = y;

        // Move to intercept ball if in defensive zone
        boolean ballInDefensiveZone = isRedTeam ?
                ball.getX() < defenseLineX + 50 :
                ball.getX() > defenseLineX - 50;

        if (ballInDefensiveZone) {
            targetY = ball.getY();
            // Adjust speed based on distance to ball
            double distToBall = Math.sqrt(Math.pow(ball.getX() - x, 2) +
                    Math.pow(ball.getY() - y, 2));
            setSpeed(Math.min(maxSpeed, distToBall * 0.1));
        }

        // Move towards target position with spacing
        moveToPosition(targetX, targetY, teammates);
    }

    /**
     * Updates attacker behavior with improved ball pursuit and obstacle avoidance
     */
    private void updateAttacker(Ball ball, List<AbstractArenaObject> obstacles,
                                List<TeamRobot> teammates) {
        double distToBall = Math.sqrt(Math.pow(ball.getX() - x, 2) +
                Math.pow(ball.getY() - y, 2));

        // Check if path to ball is clear
        boolean pathBlocked = false;
        for (AbstractArenaObject obstacle : obstacles) {
            if (detectObstacle(obstacle)) {
                pathBlocked = true;
                break;
            }
        }

        if (!pathBlocked) {
            // Approach ball with variable speed
            double approachSpeed = Math.min(maxSpeed,
                    distToBall * 0.2) * APPROACH_SLOWDOWN;
            setSpeed(approachSpeed);

            // Calculate angle to ball and turn towards it
            double angleToTarget = Math.toDegrees(Math.atan2(ball.getY() - y,
                    ball.getX() - x));
            double angleDiff = normalizeAngle(angleToTarget - rotation);

            if (Math.abs(angleDiff) > 5) {
                turn(angleDiff > 0 ? turnRate : -turnRate);
            }
        } else {
            // Find alternative path
            findAlternativePath(ball, obstacles);
        }
    }

    /**
     * Implements collision avoidance with other robots
     */
    private void avoidCollisions(List<TeamRobot> teammates, List<TeamRobot> opponents) {
        double avoidanceForceX = 0;
        double avoidanceForceY = 0;

        // Avoid teammates
        for (TeamRobot other : teammates) {
            if (other != this) {
                double dist = distanceTo(other);
                if (dist < ROBOT_SPACING) {
                    double angle = Math.atan2(y - other.getY(), x - other.getX());
                    double force = (ROBOT_SPACING - dist) / ROBOT_SPACING;
                    avoidanceForceX += Math.cos(angle) * force;
                    avoidanceForceY += Math.sin(angle) * force;
                }
            }
        }

        // Avoid opponents
        for (TeamRobot other : opponents) {
            double dist = distanceTo(other);
            if (dist < ROBOT_SPACING) {
                double angle = Math.atan2(y - other.getY(), x - other.getX());
                double force = (ROBOT_SPACING - dist) / ROBOT_SPACING;
                avoidanceForceX += Math.cos(angle) * force * 1.5; // Stronger avoidance
                avoidanceForceY += Math.sin(angle) * force * 1.5;
            }
        }

        // Apply avoidance forces
        if (Math.abs(avoidanceForceX) > 0.1 || Math.abs(avoidanceForceY) > 0.1) {
            x += avoidanceForceX * maxSpeed * 0.1;
            y += avoidanceForceY * maxSpeed * 0.1;
            brake(); // Reduce speed during avoidance
        }
    }

    /**
     * Moves robot to target position while maintaining spacing with teammates
     */
    private void moveToPosition(double targetX, double targetY,
                                List<TeamRobot> teammates) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 5) {
            // Calculate desired movement direction
            double moveX = dx / distance;
            double moveY = dy / distance;

            // Check spacing with teammates
            for (TeamRobot teammate : teammates) {
                if (teammate != this && distanceTo(teammate) < ROBOT_SPACING) {
                    // Adjust movement to maintain spacing
                    double avoidX = x - teammate.getX();
                    double avoidY = y - teammate.getY();
                    double avoidDist = Math.sqrt(avoidX * avoidX + avoidY * avoidY);
                    moveX += (avoidX / avoidDist) * 0.5;
                    moveY += (avoidY / avoidDist) * 0.5;
                }
            }

            // Apply movement
            double moveSpeed = Math.min(maxSpeed, distance * 0.1);
            x += moveX * moveSpeed;
            y += moveY * moveSpeed;

            // Update rotation to face movement direction
            rotation = Math.toDegrees(Math.atan2(moveY, moveX));
        }
    }

    /**
     * Finds alternative path when direct route is blocked
     */
    private void findAlternativePath(Ball ball, List<AbstractArenaObject> obstacles) {
        // Calculate alternative target points
        double[] leftPoint = {ball.getX() - AVOIDANCE_THRESHOLD, ball.getY()};
        double[] rightPoint = {ball.getX() + AVOIDANCE_THRESHOLD, ball.getY()};

        // Choose point with fewer obstacles
        int leftObstacles = 0;
        int rightObstacles = 0;

        for (AbstractArenaObject obstacle : obstacles) {
            if (isPointNearObstacle(leftPoint[0], leftPoint[1], obstacle)) {
                leftObstacles++;
            }
            if (isPointNearObstacle(rightPoint[0], rightPoint[1], obstacle)) {
                rightObstacles++;
            }
        }

        // Move towards better path
        double[] targetPoint = leftObstacles <= rightObstacles ? leftPoint : rightPoint;
        double angleToTarget = Math.toDegrees(Math.atan2(targetPoint[1] - y,
                targetPoint[0] - x));
        double angleDiff = normalizeAngle(angleToTarget - rotation);

        turn(angleDiff > 0 ? turnRate : -turnRate);
        setSpeed(maxSpeed * 0.5); // Reduce speed while avoiding
    }

    /**
     * Checks if a point is near an obstacle
     */
    private boolean isPointNearObstacle(double px, double py,
                                        AbstractArenaObject obstacle) {
        double dx = px - (obstacle.getX() + obstacle.getWidth()/2);
        double dy = py - (obstacle.getY() + obstacle.getHeight()/2);
        return Math.sqrt(dx*dx + dy*dy) < AVOIDANCE_THRESHOLD;
    }

    /**
     * Normalizes angle to -180 to 180 range
     */
    private double normalizeAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    // Getters and setters
    public boolean isRedTeam() { return isRedTeam; }

    public RobotRole getCurrentRole() { return currentRole; }

    public void setRole(RobotRole role) { this.currentRole = role; }
}