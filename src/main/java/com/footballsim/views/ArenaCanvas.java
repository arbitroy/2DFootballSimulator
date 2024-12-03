package com.footballsim.views;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import com.footballsim.models.Ball;

public class ArenaCanvas extends Canvas {
    private double fieldWidth = 600;
    private double fieldHeight = 400;
    private static final double GOAL_WIDTH = 60;
    private static final double BORDER_WIDTH = 20;
    
    private Ball ball;
    private AnimationTimer gameLoop;
    private boolean isRunning = false;

    public ArenaCanvas() {
        super();
        setWidth(fieldWidth + 2 * BORDER_WIDTH);
        setHeight(fieldHeight + 2 * BORDER_WIDTH);
        
        // Create ball at center of field
        ball = new Ball(getWidth()/2, getHeight()/2);
        
        // Set up mouse event handlers
        setupMouseHandlers();
        
        // Create game loop
        createGameLoop();
        
        drawField();
    }

    private void setupMouseHandlers() {
        setOnMousePressed(this::handleMousePressed);
    }

    private void handleMousePressed(MouseEvent e) {
        // Calculate force direction from ball to click
        double dx = e.getX() - ball.getX();
        double dy = e.getY() - ball.getY();
        
        // Normalize and scale force
        double length = Math.sqrt(dx*dx + dy*dy);
        if (length > 0) {
            double forceMagnitude = 2.0; // Adjust this to control kick strength
            ball.applyForce((dx/length) * forceMagnitude, (dy/length) * forceMagnitude);
        }
    }

    private void createGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateGame();
                drawGame();
            }
        };
    }

    public void startGame() {
        isRunning = true;
        gameLoop.start();
    }

    public void pauseGame() {
        isRunning = false;
        gameLoop.stop();
    }

    private void updateGame() {
        ball.update(fieldWidth, fieldHeight, BORDER_WIDTH);
    }

    private void drawGame() {
        drawField();
        ball.draw(getGraphicsContext2D());
    }

    public void setFieldDimensions(double width, double height) {
        this.fieldWidth = width;
        this.fieldHeight = height;
        setWidth(width + 2 * BORDER_WIDTH);
        setHeight(height + 2 * BORDER_WIDTH);
        drawField();
    }

    private void drawField() {
        GraphicsContext gc = getGraphicsContext2D();
        
        // Clear canvas
        gc.clearRect(0, 0, getWidth(), getHeight());
        
        // Draw border
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw field
        gc.setFill(Color.LIGHTGREEN);
        gc.fillRect(BORDER_WIDTH, BORDER_WIDTH, 
                   fieldWidth, fieldHeight);
        
        // Draw center line
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        double centerX = getWidth() / 2;
        gc.strokeLine(centerX, BORDER_WIDTH, centerX, getHeight() - BORDER_WIDTH);
        
        // Draw center circle
        double centerY = getHeight() / 2;
        double circleRadius = 50;
        gc.strokeOval(centerX - circleRadius, centerY - circleRadius, 
                     circleRadius * 2, circleRadius * 2);
        
        // Draw goals
        drawGoal(gc, true);  // Left goal
        drawGoal(gc, false); // Right goal
    }

    private void drawGoal(GraphicsContext gc, boolean isLeft) {
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(4);
        
        double x = isLeft ? BORDER_WIDTH : getWidth() - BORDER_WIDTH;
        double y = (getHeight() - GOAL_WIDTH) / 2;
        
        if (isLeft) {
            gc.strokeLine(x, y, x - 20, y); // Top
            gc.strokeLine(x - 20, y, x - 20, y + GOAL_WIDTH); // Back
            gc.strokeLine(x - 20, y + GOAL_WIDTH, x, y + GOAL_WIDTH); // Bottom
        } else {
            gc.strokeLine(x, y, x + 20, y); // Top
            gc.strokeLine(x + 20, y, x + 20, y + GOAL_WIDTH); // Back
            gc.strokeLine(x + 20, y + GOAL_WIDTH, x, y + GOAL_WIDTH); // Bottom
        }
    }
}