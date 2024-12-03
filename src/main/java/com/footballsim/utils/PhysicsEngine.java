package com.footballsim.utils;

import com.footballsim.models.*;
import java.util.*;

/**
 * Physics engine for the football simulation.
 * Handles collision detection, resolution, and physics calculations.
 */
public class PhysicsEngine {
    private static final double COLLISION_DAMPENING = 0.8;
    private static final double MIN_COLLISION_VELOCITY = 0.1;
    private static final double BALL_ROBOT_RESTITUTION = 0.7;
    private static final double ROBOT_ROBOT_RESTITUTION = 0.5;
    
    /**
     * Represents a collision between two objects
     */
    private class Collision {
        AbstractArenaObject obj1, obj2;
        double penetrationDepth;
        double[] normal;  // Normalized collision vector
        
        Collision(AbstractArenaObject obj1, AbstractArenaObject obj2) {
            this.obj1 = obj1;
            this.obj2 = obj2;
            this.normal = new double[2];
            calculateCollisionData();
        }
        
        private void calculateCollisionData() {
            // Calculate center points
            double cx1 = obj1.getX() + obj1.getWidth() / 2;
            double cy1 = obj1.getY() + obj1.getHeight() / 2;
            double cx2 = obj2.getX() + obj2.getWidth() / 2;
            double cy2 = obj2.getY() + obj2.getHeight() / 2;
            
            // Calculate collision normal
            double dx = cx2 - cx1;
            double dy = cy2 - cy1;
            double dist = Math.sqrt(dx * dx + dy * dy);
            
            if (dist > 0) {
                normal[0] = dx / dist;
                normal[1] = dy / dist;
            } else {
                normal[0] = 1;  // Default to horizontal if centers overlap
                normal[1] = 0;
            }
            
            // Calculate penetration depth
            double minDist = (obj1.getWidth() + obj2.getWidth()) / 2;
            penetrationDepth = minDist - dist;
        }
    }

    /**
     * Updates physics for all game objects
     * @param ball The game ball
     * @param robots List of all robots
     * @param obstacles List of all obstacles
     * @param fieldWidth Width of the playing field
     * @param fieldHeight Height of the playing field
     */
    public void update(Ball ball, List<TeamRobot> robots, List<AbstractArenaObject> obstacles,
                      double fieldWidth, double fieldHeight) {
        // Check and resolve ball-robot collisions
        for (TeamRobot robot : robots) {
            checkBallRobotCollision(ball, robot);
        }
        
        // Check and resolve robot-robot collisions
        for (int i = 0; i < robots.size(); i++) {
            for (int j = i + 1; j < robots.size(); j++) {
                checkRobotRobotCollision(robots.get(i), robots.get(j));
            }
        }
        
        // Check and resolve ball-obstacle collisions
        for (AbstractArenaObject obstacle : obstacles) {
            checkBallObstacleCollision(ball, obstacle);
        }
        
        // Check and resolve robot-obstacle collisions
        for (TeamRobot robot : robots) {
            for (AbstractArenaObject obstacle : obstacles) {
                checkRobotObstacleCollision(robot, obstacle);
            }
        }
        
        // Update ball position based on velocity
        updateBallPhysics(ball, fieldWidth, fieldHeight);
    }
    
    /**
     * Updates ball physics including spin and friction
     * @param ball The game ball
     * @param fieldWidth Width of the playing field
     * @param fieldHeight Height of the playing field
     */
    private void updateBallPhysics(Ball ball, double fieldWidth, double fieldHeight) {
        // Apply ground friction
        double friction = 0.98;
        double dx = ball.getDX() * friction;
        double dy = ball.getDY() * friction;
        
        // Stop ball if moving very slowly
        if (Math.abs(dx) < MIN_COLLISION_VELOCITY) dx = 0;
        if (Math.abs(dy) < MIN_COLLISION_VELOCITY) dy = 0;
        
        // Check field boundaries
        double newX = ball.getX() + dx;
        double newY = ball.getY() + dy;
        double radius = ball.getRadius();
        
        // Bounce off walls with dampening
        if (newX - radius < 0) {
            newX = radius;
            dx = -dx * COLLISION_DAMPENING;
        } else if (newX + radius > fieldWidth) {
            newX = fieldWidth - radius;
            dx = -dx * COLLISION_DAMPENING;
        }
        
        if (newY - radius < 0) {
            newY = radius;
            dy = -dy * COLLISION_DAMPENING;
        } else if (newY + radius > fieldHeight) {
            newY = fieldHeight - radius;
            dy = -dy * COLLISION_DAMPENING;
        }
        
        // Update ball position and velocity
        ball.setPosition(newX, newY);
        ball.setVelocity(dx, dy);
    }
    
    /**
     * Checks and resolves collision between ball and robot
     * @param ball The game ball
     * @param robot The robot
     */
    private void checkBallRobotCollision(Ball ball, TeamRobot robot) {
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
            double rvx = ball.getDX();
            double rvy = ball.getDY();
            
            // Calculate impulse
            double impulse = -(1 + BALL_ROBOT_RESTITUTION) * (rvx * nx + rvy * ny);
            
            // Apply impulse to ball velocity
            ball.setVelocity(
                ball.getDX() + impulse * nx,
                ball.getDY() + impulse * ny
            );
        }
    }
    
    /**
     * Checks and resolves collision between two robots
     * @param robot1 First robot
     * @param robot2 Second robot
     */
    private void checkRobotRobotCollision(TeamRobot robot1, TeamRobot robot2) {
        Collision collision = new Collision(robot1, robot2);
        
        if (collision.penetrationDepth > 0) {
            // Move robots apart
            double moveX = collision.normal[0] * collision.penetrationDepth / 2;
            double moveY = collision.normal[1] * collision.penetrationDepth / 2;
            
            // Move robot1 back
            robot1.setPosition(
                robot1.getX() - moveX,
                robot1.getY() - moveY
            );
            
            // Move robot2 forward
            robot2.setPosition(
                robot2.getX() + moveX,
                robot2.getY() + moveY
            );
            
            // Stop robots at collision point
            robot1.stop();
            robot2.stop();
        }
    }
    
    /**
     * Checks and resolves collision between ball and obstacle
     * @param ball The game ball
     * @param obstacle The obstacle
     */
    private void checkBallObstacleCollision(Ball ball, AbstractArenaObject obstacle) {
        // Create lines representing obstacle edges
        Line[] edges = createObstacleEdges(obstacle);
        
        // Check collision with each edge
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
            }
        }
    }
    
    /**
     * Checks and resolves collision between robot and obstacle
     * @param robot The robot
     * @param obstacle The obstacle
     */
    private void checkRobotObstacleCollision(TeamRobot robot, AbstractArenaObject obstacle) {
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
            }
        }
    }
    
    /**
     * Creates Line objects representing obstacle edges
     * @param obstacle The obstacle
     * @return Array of Lines representing edges
     */
    private Line[] createObstacleEdges(AbstractArenaObject obstacle) {
        double x = obstacle.getX();
        double y = obstacle.getY();
        double w = obstacle.getWidth();
        double h = obstacle.getHeight();
        
        return new Line[] {
            new Line(x, y, x + w, y),         // Top
            new Line(x + w, y, x + w, y + h), // Right
            new Line(x, y + h, x + w, y + h), // Bottom
            new Line(x, y, x, y + h)          // Left
        };
    }
    
    /**
     * Calculates normal vector for an edge
     * @param edge The edge Line
     * @return Array containing normal vector [nx, ny]
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