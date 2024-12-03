package com.footballsim.models;

import com.footballsim.utils.Line;
import java.util.*;

/**
 * Manages the football field layout, zones, and game situations
 */
public class FieldManager {
    private double fieldWidth;
    private double fieldHeight;
    private double borderWidth;
    private double goalWidth;

    // Field zones
    private Rectangle leftPenaltyArea;
    private Rectangle rightPenaltyArea;
    private Circle centerCircle;
    private Line centerLine;
    private Line[] boundaries;

    // Restart positions
    private Point centerSpot;
    private Point leftPenaltySpot;
    private Point rightPenaltySpot;
    private List<Point> cornerPositions;
    private Map<FieldSide, List<Point>> throwInPositions;

    public enum FieldSide {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    /**
     * Represents a rectangular zone on the field
     */
    public class Rectangle {
        public final double x, y, width, height;

        public Rectangle(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean contains(double px, double py) {
            return px >= x && px <= x + width &&
                    py >= y && py <= y + height;
        }
    }

    /**
     * Represents a circular zone on the field
     */
    public class Circle {
        public final double x, y, radius;

        public Circle(double x, double y, double radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        public boolean contains(double px, double py) {
            double dx = px - x;
            double dy = py - y;
            return (dx * dx + dy * dy) <= (radius * radius);
        }
    }

    /**
     * Represents a point on the field
     */
    public class Point {
        public final double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Creates a new field manager with the specified dimensions
     * 
     * @param fieldWidth  Width of the playing field
     * @param fieldHeight Height of the playing field
     * @param borderWidth Width of the border area
     * @param goalWidth   Width of the goals
     */
    public FieldManager(double fieldWidth, double fieldHeight, double borderWidth, double goalWidth) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.borderWidth = borderWidth;
        this.goalWidth = goalWidth;

        initializeField();
    }

    /**
     * Initializes field zones and positions
     */
    private void initializeField() {
        // Create penalty areas
        double penaltyWidth = fieldWidth / 6;
        double penaltyHeight = fieldHeight / 2;
        leftPenaltyArea = new Rectangle(
                borderWidth,
                borderWidth + (fieldHeight - penaltyHeight) / 2,
                penaltyWidth,
                penaltyHeight);

        rightPenaltyArea = new Rectangle(
                borderWidth + fieldWidth - penaltyWidth,
                borderWidth + (fieldHeight - penaltyHeight) / 2,
                penaltyWidth,
                penaltyHeight);

        // Create center circle
        double centerX = borderWidth + fieldWidth / 2;
        double centerY = borderWidth + fieldHeight / 2;
        double circleRadius = Math.min(fieldWidth, fieldHeight) / 8;
        centerCircle = new Circle(centerX, centerY, circleRadius);

        // Create center line
        centerLine = new Line(
                centerX, borderWidth,
                centerX, borderWidth + fieldHeight);

        // Create field boundaries
        boundaries = new Line[] {
                new Line(borderWidth, borderWidth,
                        borderWidth + fieldWidth, borderWidth), // Top
                new Line(borderWidth + fieldWidth, borderWidth,
                        borderWidth + fieldWidth, borderWidth + fieldHeight), // Right
                new Line(borderWidth, borderWidth + fieldHeight,
                        borderWidth + fieldWidth, borderWidth + fieldHeight), // Bottom
                new Line(borderWidth, borderWidth,
                        borderWidth, borderWidth + fieldHeight) // Left
        };

        // Initialize restart positions
        initializeRestartPositions();
    }

    /**
     * Initializes positions for game restarts
     */
    private void initializeRestartPositions() {
        // Center spot
        centerSpot = new Point(
                borderWidth + fieldWidth / 2,
                borderWidth + fieldHeight / 2);

        // Penalty spots
        double penaltyDistance = fieldWidth / 8;
        leftPenaltySpot = new Point(
                borderWidth + penaltyDistance,
                borderWidth + fieldHeight / 2);

        rightPenaltySpot = new Point(
                borderWidth + fieldWidth - penaltyDistance,
                borderWidth + fieldHeight / 2);

        // Corner positions
        cornerPositions = Arrays.asList(
                new Point(borderWidth, borderWidth), // Top-left
                new Point(borderWidth + fieldWidth, borderWidth), // Top-right
                new Point(borderWidth, borderWidth + fieldHeight), // Bottom-left
                new Point(borderWidth + fieldWidth, borderWidth + fieldHeight) // Bottom-right
        );

        // Throw-in positions
        initializeThrowInPositions();
    }

    /**
     * Initializes throw-in positions along field sides
     */
    private void initializeThrowInPositions() {
        throwInPositions = new HashMap<>();
        int positionsPerSide = 10;
        double xPos, yPos; // Declare variables outside switch

        // Create evenly spaced throw-in positions along each side
        for (FieldSide side : FieldSide.values()) {
            List<Point> positions = new ArrayList<>();

            switch (side) {
                case TOP:
                case BOTTOM:
                    yPos = (side == FieldSide.TOP) ? borderWidth : borderWidth + fieldHeight;
                    for (int i = 0; i < positionsPerSide; i++) {
                        xPos = borderWidth + (fieldWidth * (i + 1)) / (positionsPerSide + 1);
                        positions.add(new Point(xPos, yPos));
                    }
                    break;

                case LEFT:
                case RIGHT:
                    xPos = (side == FieldSide.LEFT) ? borderWidth : borderWidth + fieldWidth;
                    for (int i = 0; i < positionsPerSide; i++) {
                        yPos = borderWidth + (fieldHeight * (i + 1)) / (positionsPerSide + 1);
                        positions.add(new Point(xPos, yPos));
                    }
                    break;
            }

            throwInPositions.put(side, positions);
        }
    }

    /**
     * Checks if a position is out of bounds
     * 
     * @param x X coordinate to check
     * @param y Y coordinate to check
     * @return true if position is out of bounds
     */
    public boolean isOutOfBounds(double x, double y) {
        return x < borderWidth || x > borderWidth + fieldWidth ||
                y < borderWidth || y > borderWidth + fieldHeight;
    }

    /**
     * Gets the nearest throw-in position for an out of bounds position
     * 
     * @param x X coordinate of out of bounds position
     * @param y Y coordinate of out of bounds position
     * @return Nearest valid throw-in position
     */
    public Point getNearestThrowInPosition(double x, double y) {
        FieldSide side;

        // Determine which side was crossed
        if (x <= borderWidth)
            side = FieldSide.LEFT;
        else if (x >= borderWidth + fieldWidth)
            side = FieldSide.RIGHT;
        else if (y <= borderWidth)
            side = FieldSide.TOP;
        else
            side = FieldSide.BOTTOM;

        // Find nearest throw-in position on that side
        List<Point> positions = throwInPositions.get(side);
        Point nearest = positions.get(0);
        double minDist = Double.MAX_VALUE;

        for (Point pos : positions) {
            double dx = pos.x - x;
            double dy = pos.y - y;
            double dist = dx * dx + dy * dy;
            if (dist < minDist) {
                minDist = dist;
                nearest = pos;
            }
        }

        return nearest;
    }

    /**
     * Gets the nearest corner position for an out of bounds position
     * 
     * @param x X coordinate of out of bounds position
     * @param y Y coordinate of out of bounds position
     * @return Nearest corner position
     */
    public Point getNearestCornerPosition(double x, double y) {
        Point nearest = cornerPositions.get(0);
        double minDist = Double.MAX_VALUE;

        for (Point corner : cornerPositions) {
            double dx = corner.x - x;
            double dy = corner.y - y;
            double dist = dx * dx + dy * dy;
            if (dist < minDist) {
                minDist = dist;
                nearest = corner;
            }
        }

        return nearest;
    }

    /**
     * Checks if a position is within the penalty area
     * 
     * @param x        X coordinate to check
     * @param y        Y coordinate to check
     * @param leftSide true to check left penalty area, false for right
     * @return true if position is in penalty area
     */
    public boolean isInPenaltyArea(double x, double y, boolean leftSide) {
        return leftSide ? leftPenaltyArea.contains(x, y)
                : rightPenaltyArea.contains(x, y);
    }

    /**
     * Checks if a position is within the center circle
     * 
     * @param x X coordinate to check
     * @param y Y coordinate to check
     * @return true if position is in center circle
     */
    public boolean isInCenterCircle(double x, double y) {
        return centerCircle.contains(x, y);
    }

    /**
     * Gets the width of the playing field
     * 
     * @return Field width
     */
    public double getWidth() {
        return fieldWidth;
    }

    /**
     * Gets the height of the playing field
     * 
     * @return Field height
     */
    public double getHeight() {
        return fieldHeight;
    }

    // Getters
    public Rectangle getLeftPenaltyArea() {
        return leftPenaltyArea;
    }

    public Rectangle getRightPenaltyArea() {
        return rightPenaltyArea;
    }

    public Circle getCenterCircle() {
        return centerCircle;
    }

    public Point getCenterSpot() {
        return centerSpot;
    }

    public Point getLeftPenaltySpot() {
        return leftPenaltySpot;
    }

    public Point getRightPenaltySpot() {
        return rightPenaltySpot;
    }

    public List<Point> getCornerPositions() {
        return Collections.unmodifiableList(cornerPositions);
    }

    public Map<FieldSide, List<Point>> getThrowInPositions() {
        return Collections.unmodifiableMap(throwInPositions);
    }

    public Line[] getBoundaries() {
        return boundaries;
    }
}