package com.footballsim.models;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import com.footballsim.utils.Line;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an obstacle in the football arena.
 * Supports different shapes and provides collision boundaries.
 */
public class Obstacle extends AbstractArenaObject {
    
    private ObstacleType type;
    private Color color;
    private List<Line> edges;

    /**
     * Available obstacle shapes
     */
    public enum ObstacleType {
        WALL,
        CIRCLE,
        RECTANGLE
    }

    /**
     * Creates a new obstacle
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Width of the obstacle
     * @param height Height of the obstacle
     * @param type Type of obstacle
     */
    public Obstacle(double x, double y, double width, double height, ObstacleType type) {
        super(x, y, width, height);
        this.type = type;
        this.color = Color.GRAY;
        this.edges = new ArrayList<>();
        updateEdges();
    }

    /**
     * Updates the obstacle's collision boundaries
     */
    private void updateEdges() {
        edges.clear();
        
        switch (type) {
            case WALL:
            case RECTANGLE:
                // Create rectangular boundaries
                edges.add(new Line(x, y, x + width, y));           // Top
                edges.add(new Line(x + width, y, x + width, y + height)); // Right
                edges.add(new Line(x, y + height, x + width, y + height)); // Bottom
                edges.add(new Line(x, y, x, y + height));          // Left
                break;
                
            case CIRCLE:
                // Approximate circle with multiple line segments
                int segments = 16;
                double centerX = x + width/2;
                double centerY = y + height/2;
                double radius = Math.min(width, height)/2;
                
                for (int i = 0; i < segments; i++) {
                    double angle1 = (double) i / segments * 2 * Math.PI;
                    double angle2 = (double) (i + 1) / segments * 2 * Math.PI;
                    
                    double x1 = centerX + radius * Math.cos(angle1);
                    double y1 = centerY + radius * Math.sin(angle1);
                    double x2 = centerX + radius * Math.cos(angle2);
                    double y2 = centerY + radius * Math.sin(angle2);
                    
                    edges.add(new Line(x1, y1, x2, y2));
                }
                break;
        }
    }

    @Override
    public void update() {
        // Static obstacles don't need updating
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(color);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        
        switch (type) {
            case WALL:
            case RECTANGLE:
                gc.fillRect(x, y, width, height);
                gc.strokeRect(x, y, width, height);
                break;
                
            case CIRCLE:
                double centerX = x + width/2;
                double centerY = y + height/2;
                double radius = Math.min(width, height)/2;
                gc.fillOval(x, y, width, height);
                gc.strokeOval(x, y, width, height);
                break;
        }
    }

    /**
     * Gets the obstacle's edges for collision detection
     * @return List of Lines representing the obstacle's boundaries
     */
    public List<Line> getEdges() {
        return edges;
    }

    /**
     * Checks if a point intersects with this obstacle
     * @param px X coordinate of point
     * @param py Y coordinate of point
     * @return true if point intersects obstacle, false otherwise
     */
    public boolean containsPoint(double px, double py) {
        switch (type) {
            case WALL:
            case RECTANGLE:
                return px >= x && px <= x + width && 
                       py >= y && py <= y + height;
                
            case CIRCLE:
                double centerX = x + width/2;
                double centerY = y + height/2;
                double radius = Math.min(width, height)/2;
                double dx = px - centerX;
                double dy = py - centerY;
                return (dx * dx + dy * dy) <= (radius * radius);
                
            default:
                return false;
        }
    }

    /**
     * Gets the minimum distance from a point to the obstacle
     * @param px X coordinate of point
     * @param py Y coordinate of point
     * @return Minimum distance to obstacle
     */
    public double getDistanceFromPoint(double px, double py) {
        switch (type) {
            case WALL:
            case RECTANGLE:
                // Find closest edge
                double minDist = Double.MAX_VALUE;
                for (Line edge : edges) {
                    double dist = edge.distanceFrom(px, py);
                    minDist = Math.min(minDist, dist);
                }
                return minDist;
                
            case CIRCLE:
                double centerX = x + width/2;
                double centerY = y + height/2;
                double radius = Math.min(width, height)/2;
                double dx = px - centerX;
                double dy = py - centerY;
                return Math.max(0, Math.sqrt(dx * dx + dy * dy) - radius);
                
            default:
                return Double.MAX_VALUE;
        }
    }

    @Override
    public void setPosition(double x, double y) {
        super.setPosition(x, y);
        updateEdges();
    }

    // Getters and setters
    public ObstacleType getType() { return type; }
    
    public void setColor(Color color) { this.color = color; }
    public Color getColor() { return color; }
}