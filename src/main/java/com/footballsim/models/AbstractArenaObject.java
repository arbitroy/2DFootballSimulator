package com.footballsim.models;

import javafx.scene.canvas.GraphicsContext;

/**
 * Abstract base class for all objects in the football arena.
 * Provides common properties and collision detection functionality.
 */
public abstract class AbstractArenaObject {
    protected double x, y;           // Position
    protected double width, height;  // Dimensions
    protected double rotation;       // Rotation in degrees

    /**
     * Creates a new arena object at the specified position with given dimensions
     * @param x The x coordinate
     * @param y The y coordinate
     * @param width The object's width
     * @param height The object's height
     */
    public AbstractArenaObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = 0;
    }

    /**
     * Checks if this object collides with another arena object using simple rectangular collision
     * @param other The other object to check collision with
     * @return true if the objects collide, false otherwise
     */
    public boolean collidesWith(AbstractArenaObject other) {
        return !(x + width < other.x || 
                other.x + other.width < x || 
                y + height < other.y || 
                other.y + other.height < y);
    }

    /**
     * Calculates the distance to another arena object (center to center)
     * @param other The other object to calculate distance to
     * @return The distance between object centers
     */
    public double distanceTo(AbstractArenaObject other) {
        double dx = (other.x + other.width/2) - (x + width/2);
        double dy = (other.y + other.height/2) - (y + height/2);
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Draw the object on the provided graphics context
     * @param gc The graphics context to draw on
     */
    public abstract void draw(GraphicsContext gc);

    /**
     * Update the object's state for the current frame
     */
    public abstract void update();

    // Getters and setters
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { 
        this.rotation = rotation;
        // Normalize rotation to 0-360 degrees
        while (this.rotation >= 360) this.rotation -= 360;
        while (this.rotation < 0) this.rotation += 360;
    }

    /**
     * Sets the position of the object
     * @param x New x coordinate
     * @param y New y coordinate
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Gets the center coordinates of the object
     * @return Array containing [centerX, centerY]
     */
    public double[] getCenter() {
        return new double[] {
            x + width/2,
            y + height/2
        };
    }
}