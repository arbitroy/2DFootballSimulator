package com.footballsim.utils;

import com.footballsim.models.Ball;
import com.footballsim.models.Obstacle;
import com.footballsim.models.TeamRobot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;


/**
 * Static utility class for drawing debug visualization elements
 */
public class DebugVisualizer {
    private static final Color VELOCITY_COLOR = Color.RED;
    private static final Color SENSOR_COLOR = Color.LIGHTGREEN;
    private static final Color COLLISION_COLOR = Color.YELLOW;
    private static final double VELOCITY_SCALE = 5.0;
    private static final double LINE_WIDTH = 1.0;

    /**
     * Draws debug information for a robot
     * @param gc Graphics context to draw on
     * @param robot Robot to visualize
     */
    public static void drawRobotDebug(GraphicsContext gc, TeamRobot robot) {
        // Draw sensor beams
        Line[] sensorBeams = robot.createSensorBeams();
        gc.setStroke(SENSOR_COLOR);
        gc.setLineWidth(LINE_WIDTH);
        for (Line beam : sensorBeams) {
            double[] startPoint = beam.getXY(); // Gets start point (x,y)
            // Get end point coordinates from a new Line instantiation
            double endX = beam.findintersection(beam) ? beam.getXY()[0] : beam.getXY()[0];
            double endY = beam.findintersection(beam) ? beam.getXY()[1] : beam.getXY()[1];

            gc.strokeLine(startPoint[0], startPoint[1], endX, endY);
        }

        // Draw velocity vector
        gc.setStroke(VELOCITY_COLOR);
        double speed = robot.getSpeed();
        double angle = Math.toRadians(robot.getRotation());
        gc.strokeLine(
                robot.getX() + robot.getWidth()/2,
                robot.getY() + robot.getHeight()/2,
                robot.getX() + robot.getWidth()/2 + Math.cos(angle) * speed * VELOCITY_SCALE,
                robot.getY() + robot.getHeight()/2 + Math.sin(angle) * speed * VELOCITY_SCALE
        );

        // Draw collision boundary
        gc.setStroke(COLLISION_COLOR);
        gc.strokeRect(
                robot.getX(), robot.getY(),
                robot.getWidth(), robot.getHeight()
        );
    }

    /**
     * Draws debug information for the ball
     * @param gc Graphics context to draw on
     * @param ball Ball to visualize
     */
    public static void drawBallDebug(GraphicsContext gc, Ball ball) {
        // Draw velocity vector
        gc.setStroke(VELOCITY_COLOR);
        gc.setLineWidth(LINE_WIDTH);
        gc.strokeLine(
                ball.getX(), ball.getY(),
                ball.getX() + ball.getDX() * VELOCITY_SCALE,
                ball.getY() + ball.getDY() * VELOCITY_SCALE
        );

        // Draw collision circle
        gc.setStroke(COLLISION_COLOR);
        gc.strokeOval(
                ball.getX() - ball.getRadius(),
                ball.getY() - ball.getRadius(),
                ball.getRadius() * 2,
                ball.getRadius() * 2
        );
    }

    /**
     * Draws debug information for an obstacle
     * @param gc Graphics context to draw on
     * @param obstacle Obstacle to visualize
     */
    public static void drawObstacleDebug(GraphicsContext gc, Obstacle obstacle) {
        gc.setStroke(COLLISION_COLOR);
        gc.setLineWidth(LINE_WIDTH);

        // Draw collision edges
        for (Line edge : obstacle.getEdges()) {
            double[] startPoint = edge.getXY(); // Gets start coordinates
            double[] endPoints = new double[2];
            if (edge.findintersection(edge)) {
                endPoints = edge.getXY(); // Gets end coordinates if intersection exists
            }
            gc.strokeLine(startPoint[0], startPoint[1], endPoints[0], endPoints[1]);
        }
    }
}