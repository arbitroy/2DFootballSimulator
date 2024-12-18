package com.footballsim.manager;

import com.footballsim.models.Obstacle;
import com.footballsim.models.AbstractArenaObject;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages obstacles in the football simulator.
 * Handles obstacle creation, placement, collision detection, and removal.
 */
public class ObstacleManager {
    private final List<Obstacle> obstacles;
    private final double fieldWidth;
    private final double fieldHeight;
    private final double borderWidth;
    private static final double MIN_OBSTACLE_SPACING = 30.0;
    private static final double MIN_GOAL_CLEARANCE = 100.0;

    /**
     * Creates a new obstacle manager
     * 
     * @param fieldWidth  Width of the playing field
     * @param fieldHeight Height of the playing field
     * @param borderWidth Width of the field border
     */
    public ObstacleManager(double fieldWidth, double fieldHeight, double borderWidth) {
        this.obstacles = new CopyOnWriteArrayList<>();
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.borderWidth = borderWidth;
    }

    /**
     * Adds a new obstacle to the field
     * 
     * @param type   Type of obstacle to add
     * @param x      X coordinate for placement
     * @param y      Y coordinate for placement
     * @param width  Width of obstacle
     * @param height Height of obstacle
     * @return Created obstacle if placement successful, null if invalid placement
     */
    public Obstacle addObstacle(Obstacle.ObstacleType type, double x, double y,
            double width, double height) {
        // Validate placement within field bounds
        if (!isValidPlacement(x, y, width, height)) {
            return null;
        }

        Obstacle obstacle = new Obstacle(x, y, width, height, type);

        // Check for collisions with existing obstacles
        if (checkCollisions(obstacle)) {
            return null;
        }

        // Check goal area clearance
        if (!checkGoalAreaClearance(obstacle)) {
            return null;
        }

        obstacles.add(obstacle);
        return obstacle;
    }

    /**
     * Validates if placement coordinates are within field bounds
     * 
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param width  Obstacle width
     * @param height Obstacle height
     * @return true if placement is valid
     */
    private boolean isValidPlacement(double x, double y, double width, double height) {
        return x >= borderWidth &&
                x + width <= borderWidth + fieldWidth &&
                y >= borderWidth &&
                y + height <= borderWidth + fieldHeight;
    }

    /**
     * Checks if new obstacle collides with existing obstacles
     * 
     * @param newObstacle Obstacle to check
     * @return true if collision detected
     */
    private boolean checkCollisions(Obstacle newObstacle) {
        for (Obstacle obstacle : obstacles) {
            if (doObstaclesCollide(newObstacle, obstacle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if two obstacles collide including minimum spacing
     * 
     * @param obs1 First obstacle
     * @param obs2 Second obstacle
     * @return true if obstacles collide or are too close
     */
    private boolean doObstaclesCollide(Obstacle obs1, Obstacle obs2) {
        double dx = (obs1.getX() + obs1.getWidth() / 2) - (obs2.getX() + obs2.getWidth() / 2);
        double dy = (obs1.getY() + obs1.getHeight() / 2) - (obs2.getY() + obs2.getHeight() / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);

        double minDistance = Math.max(
                Math.max(obs1.getWidth(), obs1.getHeight()),
                Math.max(obs2.getWidth(), obs2.getHeight())) / 2 + MIN_OBSTACLE_SPACING;

        return distance < minDistance;
    }

    /**
     * Checks if obstacle maintains required clearance from goal areas
     * 
     * @param obstacle Obstacle to check
     * @return true if goal area clearance is maintained
     */
    private boolean checkGoalAreaClearance(Obstacle obstacle) {
        // Calculate goal positions
        double leftGoalX = borderWidth;
        double rightGoalX = borderWidth + fieldWidth;
        double goalY = (fieldHeight - 60) / 2 + borderWidth; // 60 is standard goal width

        // Check distance to left goal
        double distToLeftGoal = Math.abs(obstacle.getX() - leftGoalX);
        if (distToLeftGoal < MIN_GOAL_CLEARANCE &&
                obstacle.getY() + obstacle.getHeight() > goalY &&
                obstacle.getY() < goalY + 60) {
            return false;
        }

        // Check distance to right goal
        double distToRightGoal = Math.abs(obstacle.getX() + obstacle.getWidth() - rightGoalX);
        if (distToRightGoal < MIN_GOAL_CLEARANCE &&
                obstacle.getY() + obstacle.getHeight() > goalY &&
                obstacle.getY() < goalY + 60) {
            return false;
        }

        return true;
    }

    /**
     * Removes an obstacle from the field
     * 
     * @param obstacle Obstacle to remove
     */
    public void removeObstacle(Obstacle obstacle) {
        obstacles.remove(obstacle);
    }

    /**
     * Removes all obstacles from the field
     */
    public void clearObstacles() {
        obstacles.clear();
    }

    /**
     * Gets obstacle at specified coordinates
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return Obstacle at position, or null if none found
     */
    public Obstacle getObstacleAt(double x, double y) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.containsPoint(x, y)) {
                return obstacle;
            }
        }
        return null;
    }

    /**
     * Gets all current obstacles
     * 
     * @return Unmodifiable list of obstacles
     */
    public List<Obstacle> getObstacles() {
        return Collections.unmodifiableList(obstacles);
    }

    /**
     * Gets obstacles as arena objects for rendering
     * 
     * @return List of obstacles as arena objects
     */
    public List<AbstractArenaObject> getObstaclesAsArenaObjects() {
        return new ArrayList<>(obstacles);
    }

    /**
     * Updates all obstacle states
     */
    public void update() {
        for (Obstacle obstacle : obstacles) {
            obstacle.update();
        }
    }
}