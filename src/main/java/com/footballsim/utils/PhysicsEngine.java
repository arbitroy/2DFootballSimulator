package com.footballsim.utils;

import com.footballsim.models.*;
import java.util.*;

/**
 * Physics engine for the football simulation.
 * Handles collision detection, resolution, and physics calculations.
 */
public class PhysicsEngine {
    // Physics constants
    private static final double COLLISION_DAMPENING = 0.8;
    private static final double MIN_COLLISION_VELOCITY = 0.1;
    private static final double BALL_ROBOT_RESTITUTION = 0.7;
    private static final double ROBOT_ROBOT_RESTITUTION = 0.5;
    private static final int MAX_COLLISION_ITERATIONS = 10;
    private static final double MIN_SEPARATION_VELOCITY = 0.1;
    private static final double MIN_SEPARATION_DISTANCE = 0.01;
    
    /**
     * Updates physics for all game objects with collision iteration limiting
     */
    public void update(Ball ball, List<TeamRobot> robots, List<AbstractArenaObject> obstacles,
                      double fieldWidth, double fieldHeight) {
        int iterationCount = 0;
        boolean hasCollisions;

        do {
            hasCollisions = false;
            
            // Check ball-robot collisions
            for (TeamRobot robot : robots) {
                if (checkBallRobotCollision(ball, robot)) {
                    hasCollisions = true;
                }
            }
            
            // Check robot-robot collisions
            for (int i = 0; i < robots.size(); i++) {
                for (int j = i + 1; j < robots.size(); j++) {
                    if (checkRobotRobotCollision(robots.get(i), robots.get(j))) {
                        hasCollisions = true;
                    }
                }
            }
            
            // Check obstacle collisions
            for (AbstractArenaObject obstacle : obstacles) {
                if (checkBallObstacleCollision(ball, obstacle)) {
                    hasCollisions = true;
                }
                for (TeamRobot robot : robots) {
                    if (checkRobotObstacleCollision(robot, obstacle)) {
                        hasCollisions = true;
                    }
                }
            }

            iterationCount++;
            
            // Update positions after collision resolution
            updatePositions(ball, robots);
            
        } while (hasCollisions && iterationCount < MAX_COLLISION_ITERATIONS);

        // If we hit max iterations, apply separation force
        if (iterationCount >= MAX_COLLISION_ITERATIONS) {
            forceSeparation(ball, robots);
        }

        // Update final physics state
        updateBallPhysics(ball, fieldWidth, fieldHeight);
    }

    /**
     * Checks and resolves collision between ball and robot
     * @return true if collision occurred
     */
    private boolean checkBallRobotCollision(Ball ball, TeamRobot robot) {
        // Calculate distance between centers
        double dx = ball.getX() - (robot.getX() + robot.getWidth() / 2);
        double dy = ball.getY() - (robot.getY() + robot.getHeight() / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        double minDist = ball.getRadius() + robot.getWidth() / 2;
        
        if (distance < minDist) {
            // Collision detected - calculate response
            double overlap = minDist - distance;
            
            // Normalize collision vector
            double nx = dx / distance;
            double ny = dy / distance;
            
            // Move ball out of collision
            ball.setPosition(
                ball.getX() + nx * overlap,
                ball.getY() + ny * overlap
            );
            
            // Calculate relative velocity
            double rvx = ball.getDX() - robot.getSpeed() * Math.cos(Math.toRadians(robot.getRotation()));
            double rvy = ball.getDY() - robot.getSpeed() * Math.sin(Math.toRadians(robot.getRotation()));
            
            // Calculate impulse
            double impulse = -(1 + BALL_ROBOT_RESTITUTION) * (rvx * nx + rvy * ny);
            impulse /= (1/ball.getRadius() + 1/robot.getMass());
            
            // Apply impulse to ball velocity
            ball.setVelocity(
                ball.getDX() + impulse * nx / ball.getRadius(),
                ball.getDY() + impulse * ny / ball.getRadius()
            );

            return true;
        }
        return false;
    }

    /**
     * Checks and resolves collision between two robots
     * @return true if collision occurred
     */
    private boolean checkRobotRobotCollision(TeamRobot robot1, TeamRobot robot2) {
        double dx = robot2.getX() - robot1.getX();
        double dy = robot2.getY() - robot1.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        double minDist = (robot1.getWidth() + robot2.getWidth()) / 2;
        
        if (distance < minDist) {
            // Calculate overlap
            double overlap = minDist - distance;
            
            // Normalize collision vector
            double nx = dx / distance;
            double ny = dy / distance;
            
            // Move robots apart
            double moveX = nx * overlap / 2;
            double moveY = ny * overlap / 2;
            
            robot1.setPosition(
                robot1.getX() - moveX,
                robot1.getY() - moveY
            );
            
            robot2.setPosition(
                robot2.getX() + moveX,
                robot2.getY() + moveY
            );
            
            // Calculate relative velocity
            double rvx = robot2.getSpeed() * Math.cos(Math.toRadians(robot2.getRotation())) -
                        robot1.getSpeed() * Math.cos(Math.toRadians(robot1.getRotation()));
            double rvy = robot2.getSpeed() * Math.sin(Math.toRadians(robot2.getRotation())) -
                        robot1.getSpeed() * Math.sin(Math.toRadians(robot1.getRotation()));
            
            // If moving toward each other, stop both robots
            if (rvx * nx + rvy * ny < 0) {
                robot1.stop();
                robot2.stop();
            }

            return true;
        }
        return false;
    }

    /**
     * Checks and resolves collision between ball and obstacle
     * @return true if collision occurred
     */
    private boolean checkBallObstacleCollision(Ball ball, AbstractArenaObject obstacle) {
        if (obstacle instanceof Obstacle) {
            Obstacle obs = (Obstacle) obstacle;
            
            // Get obstacle edges
            List<Line> edges = obs.getEdges();
            for (Line edge : edges) {
                double distance = edge.distanceFrom(ball.getX(), ball.getY());
                
                if (distance < ball.getRadius()) {
                    // Calculate reflection vector
                    double[] normal = calculateEdgeNormal(edge);
                    
                    // Calculate reflection velocity
                    double dotProduct = ball.getDX() * normal[0] + ball.getDY() * normal[1];
                    double reflectX = ball.getDX() - 2 * dotProduct * normal[0];
                    double reflectY = ball.getDY() - 2 * dotProduct * normal[1];
                    
                    // Apply dampening and update ball velocity
                    ball.setVelocity(
                        reflectX * COLLISION_DAMPENING,
                        reflectY * COLLISION_DAMPENING
                    );
                    
                    // Move ball out of collision
                    double overlap = ball.getRadius() - distance;
                    ball.setPosition(
                        ball.getX() + normal[0] * overlap,
                        ball.getY() + normal[1] * overlap
                    );

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks and resolves collision between robot and obstacle
     * @return true if collision occurred
     */
    private boolean checkRobotObstacleCollision(TeamRobot robot, AbstractArenaObject obstacle) {
        if (robot.collidesWith(obstacle)) {
            // Calculate overlap and direction
            double dx = (robot.getX() + robot.getWidth()/2) - 
                       (obstacle.getX() + obstacle.getWidth()/2);
            double dy = (robot.getY() + robot.getHeight()/2) - 
                       (obstacle.getY() + obstacle.getHeight()/2);
            
            // Normalize direction
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance > 0) {
                dx /= distance;
                dy /= distance;
                
                // Move robot out of collision
                double overlap = (robot.getWidth() + obstacle.getWidth())/2 - distance;
                robot.setPosition(
                    robot.getX() + dx * overlap,
                    robot.getY() + dy * overlap
                );
                
                // Stop robot at collision point
                robot.stop();
                return true;
            }
        }
        return false;
    }

    /**
     * Forces separation between objects that are still colliding
     */
    private void forceSeparation(Ball ball, List<TeamRobot> robots) {
        for (TeamRobot robot : robots) {
            // Force separation between ball and robot if still colliding
            double dx = ball.getX() - robot.getX();
            double dy = ball.getY() - robot.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < MIN_SEPARATION_DISTANCE) {
                // Apply minimum separation
                double angle = Math.atan2(dy, dx);
                ball.setPosition(
                    robot.getX() + Math.cos(angle) * MIN_SEPARATION_DISTANCE,
                    robot.getY() + Math.sin(angle) * MIN_SEPARATION_DISTANCE
                );
                // Add minimum separation velocity
                ball.setVelocity(
                    Math.cos(angle) * MIN_SEPARATION_VELOCITY,
                    Math.sin(angle) * MIN_SEPARATION_VELOCITY
                );
            }
        }

        // Separate robots from each other
        for (int i = 0; i < robots.size(); i++) {
            for (int j = i + 1; j < robots.size(); j++) {
                TeamRobot r1 = robots.get(i);
                TeamRobot r2 = robots.get(j);
                
                double dx = r2.getX() - r1.getX();
                double dy = r2.getY() - r1.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance < MIN_SEPARATION_DISTANCE) {
                    double angle = Math.atan2(dy, dx);
                    r1.setPosition(
                        r1.getX() - Math.cos(angle) * MIN_SEPARATION_DISTANCE/2,
                        r1.getY() - Math.sin(angle) * MIN_SEPARATION_DISTANCE/2
                    );
                    r2.setPosition(
                        r2.getX() + Math.cos(angle) * MIN_SEPARATION_DISTANCE/2,
                        r2.getY() + Math.sin(angle) * MIN_SEPARATION_DISTANCE/2
                    );
                    // Stop both robots
                    r1.stop();
                    r2.stop();
                }
            }
        }
    }

    /**
     * Updates positions of all objects after collision resolution
     */
    private void updatePositions(Ball ball, List<TeamRobot> robots) {
        // Update ball position based on velocity
        ball.setPosition(
            ball.getX() + ball.getDX(),
            ball.getY() + ball.getDY()
        );
        
        // Update robot positions
        for (TeamRobot robot : robots) {
            robot.update();
        }
    }

    /**
     * Updates ball physics including friction and boundaries
     */
    private void updateBallPhysics(Ball ball, double fieldWidth, double fieldHeight) {
        // Apply ground friction
        double friction = 0.98;
        double dx = ball.getDX() * friction;
        double dy = ball.getDY() * friction;
        
        // Stop ball if moving very slowly
        if (Math.abs(dx) < MIN_COLLISION_VELOCITY) dx = 0;
        if (Math.abs(dy) < MIN_COLLISION_VELOCITY) dy = 0;
        
        // Update ball velocity
        ball.setVelocity(dx, dy);
    }

    /**
     * Calculates normal vector for an edge
     */
    private double[] calculateEdgeNormal(Line edge) {
        double[] xy = edge.getXY();
        double dx = xy[2] - xy[0];
        double dy = xy[3] - xy[1];
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length > 0) {
            // Return normalized perpendicular vector
            return new double[] {-dy / length, dx / length};
        }
        
        return new double[] {1, 0}; // Default to horizontal if line has no length
    }
}