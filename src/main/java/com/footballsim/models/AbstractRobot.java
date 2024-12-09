package com.footballsim.models;

import com.footballsim.utils.Line;

import java.util.List;

/**
 * Abstract base class for all robots in the game.
 * Implements basic movement, physics and sensor capabilities.
 */
public abstract class AbstractRobot extends AbstractArenaObject {
    // Movement properties
    protected double speed;          // Current speed
    protected double maxSpeed;       // Maximum speed
    protected double acceleration;   // Acceleration rate
    protected double turnRate;       // Turn rate in degrees per update
    
    // Sensor properties
    protected double sensorRange;    // Range of obstacle detection sensors
    protected double sensorAngle;    // Field of view angle for sensors
    protected static final int NUM_SENSORS = 5;  // Number of sensor beams
    
    // Physics properties
    protected double mass;           // Robot mass for collision calculations
    protected double friction;       // Surface friction coefficient
    protected boolean isColliding;   // Collision state flag

    /**
     * Creates a new robot with specified properties
     * @param x Starting x coordinate
     * @param y Starting y coordinate
     * @param maxSpeed Maximum movement speed
     * @param sensorRange Range of obstacle detection
     */
    public AbstractRobot(double x, double y, double maxSpeed, double sensorRange) {
        super(x, y, 20, 20);  // Standard robot size
        this.maxSpeed = maxSpeed;
        this.sensorRange = sensorRange;
        this.sensorAngle = 120;  // 120 degree field of view
        
        // Initialize physics properties
        this.speed = 0;
        this.acceleration = 0.5;
        this.turnRate = 5.0;
        this.mass = 1.0;
        this.friction = 0.98;
        this.isColliding = false;
    }

    /**
     * Updates the robot's behavior based on game state
     * @param ball The game ball
     * @param obstacles List of obstacles in the arena
     * @param teammates List of robots on the same team
     * @param opponents List of robots on the opposing team
     */
    public abstract void updateBehavior(Ball ball,
                                        List<AbstractArenaObject> obstacles,
                                        List<TeamRobot> teammates,
                                        List<TeamRobot> opponents);

    /**
     * Updates the robot's position and state
     */
    @Override
    public void update() {
        if (!isColliding) {
            // Update position based on current speed and rotation
            double dx = speed * Math.cos(Math.toRadians(rotation));
            double dy = speed * Math.sin(Math.toRadians(rotation));
            
            x += dx;
            y += dy;
            
            // Apply friction
            speed *= friction;
            
            // Stop if moving very slowly
            if (Math.abs(speed) < 0.01) {
                speed = 0;
            }
        }
    }

    /**
     * Creates sensor beams for obstacle detection
     * @return Array of Lines representing sensor beams
     */
    public Line[] createSensorBeams() {
        Line[] beams = new Line[NUM_SENSORS];
        double[] center = getCenter();
        
        // Calculate spread of beams within sensor angle
        double angleStep = sensorAngle / (NUM_SENSORS - 1);
        double startAngle = rotation - sensorAngle/2;
        
        for (int i = 0; i < NUM_SENSORS; i++) {
            double beamAngle = startAngle + (angleStep * i);
            double endX = center[0] + sensorRange * Math.cos(Math.toRadians(beamAngle));
            double endY = center[1] + sensorRange * Math.sin(Math.toRadians(beamAngle));
            
            beams[i] = new Line(center[0], center[1], endX, endY);
        }
        
        return beams;
    }

    /**
     * Checks if an obstacle is detected by any sensor beam
     * @param obstacle The obstacle to check
     * @return true if the obstacle is detected, false otherwise
     */
    protected boolean detectObstacle(AbstractArenaObject obstacle) {
        Line[] sensorBeams = createSensorBeams();
        
        // Create lines representing obstacle edges
        Line[] obstacleEdges = createObstacleEdges(obstacle);
        
        // Check each sensor beam against each obstacle edge
        for (Line beam : sensorBeams) {
            for (Line edge : obstacleEdges) {
                if (beam.findintersection(edge)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Creates lines representing the edges of an obstacle
     * @param obstacle The obstacle to create edges for
     * @return Array of Lines representing obstacle edges
     */
    private Line[] createObstacleEdges(AbstractArenaObject obstacle) {
        double ox = obstacle.getX();
        double oy = obstacle.getY();
        double ow = obstacle.getWidth();
        double oh = obstacle.getHeight();
        
        return new Line[] {
            new Line(ox, oy, ox + ow, oy),         // Top
            new Line(ox + ow, oy, ox + ow, oy + oh), // Right
            new Line(ox, oy + oh, ox + ow, oy + oh), // Bottom
            new Line(ox, oy, ox, oy + oh)          // Left
        };
    }

    /**
     * Accelerates the robot forward
     */
    public void accelerate() {
        speed = Math.min(speed + acceleration, maxSpeed);
    }

    /**
     * Applies braking to slow the robot
     */
    public void brake() {
        speed = Math.max(speed - acceleration * 2, 0);
    }

    /**
     * Turns the robot by the specified angle
     * @param angle Angle to turn (positive for right, negative for left)
     */
    public void turn(double angle) {
        rotation += Math.max(Math.min(angle, turnRate), -turnRate);
        while (rotation >= 360) rotation -= 360;
        while (rotation < 0) rotation += 360;
    }

    /**
     * Sets the collision state of the robot
     * @param colliding true if robot is colliding, false otherwise
     */
    public void setColliding(boolean colliding) {
        this.isColliding = colliding;
    }


    /**
     * Stops the robot's movement immediately
     */
    public void stop() {
        this.speed = 0;
    }

    // Getters and setters
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = Math.min(speed, maxSpeed); }
    
    public double getMaxSpeed() { return maxSpeed; }
    public double getAcceleration() { return acceleration; }
    public double getSensorRange() { return sensorRange; }
    public double getMass() { return mass; }
}