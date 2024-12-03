package com.footballsim.models;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import com.footballsim.utils.Line;

public class Ball {
    private double x, y;          // Position
    private double dx, dy;        // Velocity
    private double radius = 5;    // Ball radius
    private double friction = 0.98; // Friction coefficient
    private static final double MAX_SPEED = 10;

    public Ball(double x, double y) {
        this.x = x;
        this.y = y;
        this.dx = 0;
        this.dy = 0;
    }

    public void update(double fieldWidth, double fieldHeight, double borderWidth) {
        // Update position
        x += dx;
        y += dy;

        // Apply friction
        dx *= friction;
        dy *= friction;

        // Stop if moving very slowly
        if (Math.abs(dx) < 0.01) dx = 0;
        if (Math.abs(dy) < 0.01) dy = 0;

        // Boundary collisions
        handleBoundaryCollision(fieldWidth, fieldHeight, borderWidth);
    }

    private void handleBoundaryCollision(double fieldWidth, double fieldHeight, double borderWidth) {
        // Left and right boundaries
        if (x - radius < borderWidth) {
            x = borderWidth + radius;
            dx = -dx * 0.8; // Bounce with energy loss
        } else if (x + radius > fieldWidth + borderWidth) {
            x = fieldWidth + borderWidth - radius;
            dx = -dx * 0.8;
        }

        // Top and bottom boundaries
        if (y - radius < borderWidth) {
            y = borderWidth + radius;
            dy = -dy * 0.8;
        } else if (y + radius > fieldHeight + borderWidth) {
            y = fieldHeight + borderWidth - radius;
            dy = -dy * 0.8;
        }
    }

    public void applyForce(double fx, double fy) {
        dx += fx;
        dy += fy;
        
        // Limit maximum speed
        double speed = Math.sqrt(dx*dx + dy*dy);
        if (speed > MAX_SPEED) {
            dx = (dx/speed) * MAX_SPEED;
            dy = (dy/speed) * MAX_SPEED;
        }
    }

    public void draw(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    /**
     * Sets the ball's position
     * @param x New x coordinate
     * @param y New y coordinate
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the ball's velocity
     * @param dx New x velocity component
     * @param dy New y velocity component
     */
    public void setVelocity(double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Get the current velocity as a 2D array [dx, dy]
     * @return Array containing [dx, dy]
     */
    public double[] getVelocity() {
        return new double[]{dx, dy};
    }

    /**
     * Get the current position as a 2D array [x, y]
     * @return Array containing [x, y]
     */
    public double[] getPosition() {
        return new double[]{x, y};
    }

    // Existing getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getDX() { return dx; }
    public double getDY() { return dy; }
    public double getRadius() { return radius; }
}