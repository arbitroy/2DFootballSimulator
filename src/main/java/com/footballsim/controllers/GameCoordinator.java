package com.footballsim.controllers;

import com.footballsim.models.*;
import com.footballsim.utils.Line;
import com.footballsim.views.*;
import com.footballsim.formations.FormationManager;
import javafx.animation.AnimationTimer;
import javafx.scene.input.MouseEvent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import java.util.*;

/**
 * Coordinates interactions between game components and UI elements.
 * Acts as a central hub for managing game state, UI updates, and user
 * interactions.
 */
public class GameCoordinator {
    // Core components
    private final GameController gameController;
    private final ArenaCanvas arenaCanvas;
    private FieldManager fieldManager;
    private final FormationManager redTeamFormation;
    private final FormationManager blueTeamFormation;
    private final GameSettingsPanel settingsPanel;
    private final RobotControlPanel robotPanel;

    // Constants for rendering
    private static final Color FIELD_COLOR = Color.LIGHTGREEN;
    private static final Color BORDER_COLOR = Color.DARKGREEN;
    private static final Color LINE_COLOR = Color.WHITE;
    private static final double LINE_WIDTH = 2.0;
    private static final Color SELECTION_COLOR = Color.YELLOW;
    private static final double SELECTION_WIDTH = 2.0;

    // Game state
    private TeamRobot selectedRobot;
    private boolean isDraggingRobot;
    private double dragStartX, dragStartY;
    private AnimationTimer gameLoop;
    private boolean showDebugInfo;
    private boolean gameRunning = false;

    /**
     * Creates a new game coordinator with the specified components
     */
    public GameCoordinator(ArenaCanvas arenaCanvas, GameSettingsPanel settingsPanel,
            RobotControlPanel robotPanel) {
        this.arenaCanvas = arenaCanvas;
        this.settingsPanel = settingsPanel;
        this.robotPanel = robotPanel;

        // Initialize core components
        this.fieldManager = new FieldManager(arenaCanvas.getWidth(), arenaCanvas.getHeight(), 20, 60);
        this.gameController = new GameController(fieldManager.getWidth(), fieldManager.getHeight());
        this.redTeamFormation = new FormationManager(fieldManager, true);
        this.blueTeamFormation = new FormationManager(fieldManager, false);
        this.showDebugInfo = false;

        setupEventHandlers();
        initializeGameLoop();
    }

    /**
     * Clears the canvas
     */
    private void clearCanvas(GraphicsContext gc) {
        gc.setFill(BORDER_COLOR);
        gc.fillRect(0, 0, arenaCanvas.getWidth(), arenaCanvas.getHeight());
    }

    /**
     * Draws the field including all markings
     */
    private void drawField(GraphicsContext gc) {
        // Clear and draw main field background
        gc.setFill(FIELD_COLOR);
        gc.fillRect(fieldManager.getBoundaries()[0].getXY()[0],
                fieldManager.getBoundaries()[0].getXY()[1],
                fieldManager.getWidth(),
                fieldManager.getHeight());

        gc.setStroke(LINE_COLOR);
        gc.setLineWidth(LINE_WIDTH);

        // Draw field boundaries
        for (Line boundary : fieldManager.getBoundaries()) {
            double[] xy = boundary.getXY();
            // Each line has start (x,y) and end (x,y) coordinates
            gc.strokeLine(xy[0], xy[1], xy[2], xy[3]);
        }

        // Draw center line
        double centerX = fieldManager.getCenterSpot().x;
        double topY = fieldManager.getBoundaries()[0].getXY()[1];
        double bottomY = topY + fieldManager.getHeight();
        gc.strokeLine(centerX, topY, centerX, bottomY);

        // Draw center circle
        FieldManager.Circle centerCircle = fieldManager.getCenterCircle();
        gc.strokeOval(centerCircle.x - centerCircle.radius,
                centerCircle.y - centerCircle.radius,
                centerCircle.radius * 2,
                centerCircle.radius * 2);

        // Draw center spot
        double spotRadius = 3;
        gc.setFill(LINE_COLOR);
        gc.fillOval(fieldManager.getCenterSpot().x - spotRadius,
                fieldManager.getCenterSpot().y - spotRadius,
                spotRadius * 2, spotRadius * 2);

        // Draw penalty areas
        drawPenaltyArea(gc, fieldManager.getLeftPenaltyArea());
        drawPenaltyArea(gc, fieldManager.getRightPenaltyArea());

        // Draw goals
        drawGoal(gc, true); // Left goal
        drawGoal(gc, false); // Right goal
    }

    /**
     * Draws a penalty area
     */
    private void drawPenaltyArea(GraphicsContext gc, FieldManager.Rectangle area) {
        gc.setStroke(LINE_COLOR);
        gc.strokeRect(area.x, area.y, area.width, area.height);

        // Draw penalty spot
        double spotRadius = 3;
        gc.setFill(LINE_COLOR);
        if (area == fieldManager.getLeftPenaltyArea()) {
            gc.fillOval(fieldManager.getLeftPenaltySpot().x - spotRadius,
                    fieldManager.getLeftPenaltySpot().y - spotRadius,
                    spotRadius * 2, spotRadius * 2);
        } else {
            gc.fillOval(fieldManager.getRightPenaltySpot().x - spotRadius,
                    fieldManager.getRightPenaltySpot().y - spotRadius,
                    spotRadius * 2, spotRadius * 2);
        }
    }

    /**
     * Draws a goal
     */
    private void drawGoal(GraphicsContext gc, boolean isLeft) {
        double goalDepth = 20;
        double x = isLeft ? fieldManager.getBoundaries()[0].getXY()[0]
                : fieldManager.getBoundaries()[1].getXY()[0];
        double y = (fieldManager.getHeight() - 60) / 2 + fieldManager.getBoundaries()[0].getXY()[1];

        gc.setStroke(LINE_COLOR);
        gc.setLineWidth(LINE_WIDTH * 2); // Make goals more visible

        if (isLeft) {
            gc.strokeLine(x, y, x - goalDepth, y); // Top
            gc.strokeLine(x - goalDepth, y, x - goalDepth, y + 60); // Back
            gc.strokeLine(x - goalDepth, y + 60, x, y + 60); // Bottom
        } else {
            gc.strokeLine(x, y, x + goalDepth, y); // Top
            gc.strokeLine(x + goalDepth, y, x + goalDepth, y + 60); // Back
            gc.strokeLine(x + goalDepth, y + 60, x, y + 60); // Bottom
        }

        gc.setLineWidth(LINE_WIDTH); // Reset line width
    }

    /**
     * Draws the ball
     */
    private void drawBall(GraphicsContext gc) {
        Ball ball = gameController.getBall();
        ball.draw(gc);

        if (showDebugInfo) {
            // Draw velocity vector
            gc.setStroke(Color.RED);
            gc.setLineWidth(1);
            double[] velocity = ball.getVelocity();
            gc.strokeLine(ball.getX(), ball.getY(),
                    ball.getX() + velocity[0] * 5,
                    ball.getY() + velocity[1] * 5);
        }
    }

    /**
     * Draws all robots
     */
    private void drawRobots(GraphicsContext gc) {
        for (TeamRobot robot : gameController.getRedTeam()) {
            robot.draw(gc);
        }
        for (TeamRobot robot : gameController.getBlueTeam()) {
            robot.draw(gc);
        }
    }

    /**
     * Draws all obstacles
     */
    private void drawObstacles(GraphicsContext gc) {
        for (AbstractArenaObject obstacle : gameController.getObstacles()) {
            if (obstacle instanceof Obstacle) {
                ((Obstacle) obstacle).draw(gc);
            }
        }
    }

    /**
     * Draws selection indicator around selected robot
     */
    private void drawSelectionIndicator(GraphicsContext gc, TeamRobot robot) {
        gc.setStroke(SELECTION_COLOR);
        gc.setLineWidth(SELECTION_WIDTH);
        double padding = 2;
        gc.strokeRect(robot.getX() - padding,
                robot.getY() - padding,
                robot.getWidth() + padding * 2,
                robot.getHeight() + padding * 2);

        // Draw role indicator
        gc.setFill(SELECTION_COLOR);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(robot.getCurrentRole().toString(),
                robot.getX() + robot.getWidth() / 2,
                robot.getY() - 5);
    }

    /**
     * Sets up mouse and interaction event handlers
     */
    private void setupEventHandlers() {
        // Mouse handlers for robot placement and ball kicks
        arenaCanvas.setOnMousePressed(this::handleMousePressed);
        arenaCanvas.setOnMouseDragged(this::handleMouseDragged);
        arenaCanvas.setOnMouseReleased(this::handleMouseReleased);

        // Game controller event listeners
        gameController.addEventListener(new GameController.GameEventListener() {
            @Override
            public void onGoalScored(boolean redTeam) {
                settingsPanel.updateScore(gameController.getRedScore(), gameController.getBlueScore());
            }

            @Override
            public void onTimeUpdated(int minutes, int seconds) {
                settingsPanel.updateTimer(minutes, seconds);
            }

            @Override
            public void onGameEnd(boolean redTeamWon) {
                stopGame();
                showGameEndDialog(redTeamWon);
            }
        });
    }

    /**
     * Initializes the game loop for continuous updates
     */
    private void initializeGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
    }

    /**
     * Updates game state
     */
    private void update() {
        gameController.update();
        updateFormations();
    }

    /**
     * Updates team formations
     */
    private void updateFormations() {
        redTeamFormation.assignPositions(gameController.getRedTeam());
        blueTeamFormation.assignPositions(gameController.getBlueTeam());
    }

    /**
     * Renders the game state
     */
    private void render() {
        GraphicsContext gc = arenaCanvas.getGraphicsContext2D();
        clearCanvas(gc);

        // Draw field
        drawField(gc);

        // Draw game objects
        drawBall(gc);
        drawRobots(gc);
        drawObstacles(gc);

        // Draw selection indicators
        if (selectedRobot != null) {
            drawSelectionIndicator(gc, selectedRobot);
        }
    }

    /**
     * Handles mouse press events
     * 
     * @param event Mouse event
     */
    private void handleMousePressed(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        // Check for robot selection
        TeamRobot clickedRobot = findRobotAt(x, y);
        if (clickedRobot != null) {
            selectedRobot = clickedRobot;
            isDraggingRobot = true;
            dragStartX = x - clickedRobot.getX();
            dragStartY = y - clickedRobot.getY();
            return;
        }

        // Handle ball kicks
        Ball ball = gameController.getBall();
        double dx = x - ball.getX();
        double dy = y - ball.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 0) {
            double power = Math.min(dist / 100.0, 1.0) * 5.0; // Scale kick power
            ball.applyForce((dx / dist) * power, (dy / dist) * power);
        }
    }

    /**
     * Handles mouse drag events
     * 
     * @param event Mouse event
     */
    private void handleMouseDragged(MouseEvent event) {
        if (isDraggingRobot && selectedRobot != null) {
            double newX = event.getX() - dragStartX;
            double newY = event.getY() - dragStartY;

            // Check field boundaries
            if (!fieldManager.isOutOfBounds(newX, newY)) {
                selectedRobot.setPosition(newX, newY);
            }
        }
    }

    /**
     * Handles mouse release events
     * 
     * @param event Mouse event
     */
    private void handleMouseReleased(MouseEvent event) {
        isDraggingRobot = false;
    }

    /**
     * Finds a robot at the specified coordinates
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return Robot at position, or null if none found
     */
    private TeamRobot findRobotAt(double x, double y) {
        for (TeamRobot robot : gameController.getRedTeam()) {
            if (isPointInRobot(x, y, robot))
                return robot;
        }
        for (TeamRobot robot : gameController.getBlueTeam()) {
            if (isPointInRobot(x, y, robot))
                return robot;
        }
        return null;
    }

    /**
     * Checks if a point is within a robot's bounds
     * 
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param robot Robot to check
     * @return true if point is in robot
     */
    private boolean isPointInRobot(double x, double y, TeamRobot robot) {
        return x >= robot.getX() && x <= robot.getX() + robot.getWidth() &&
                y >= robot.getY() && y <= robot.getY() + robot.getHeight();
    }

    /**
     * Removes the currently selected object (robot or obstacle)
     */
    public void removeSelectedObject() {
        if (selectedRobot != null) {
            if (selectedRobot.isRedTeam()) {
                gameController.getRedTeam().remove(selectedRobot);
            } else {
                gameController.getBlueTeam().remove(selectedRobot);
            }
            selectedRobot = null;
        }
    }

    /**
     * Shows game end dialog
     * 
     * @param redTeamWon true if red team won
     */
    private void showGameEndDialog(boolean redTeamWon) {
        // TODO: Implement game end dialog
    }

    /**
     * Checks if the game is currently running
     * 
     * @return true if game is running
     */
    public boolean isGameRunning() {
        return gameRunning;
    }

    /**
     * Starts the game
     */
    public void startGame() {
        gameLoop.start();
        gameController.startGame();
        gameRunning = true;
    }

    /**
     * Stops the game
     */
    public void stopGame() {
        gameLoop.stop();
        gameController.pauseGame();
        gameRunning = false;
    }

    /**
     * Updates the field dimensions
     * 
     * @param width  New field width
     * @param height New field height
     */
    public void updateFieldDimensions(Double width, Double height) {
        // Recreate field manager with new dimensions
        this.fieldManager = new FieldManager(width, height, 20, 60);
        // Update game controller with new dimensions
        this.gameController.loadConfig(new GameConfig() {
            {
                setFieldWidth(width);
                setFieldHeight(height);
            }
        });
        // Trigger a render
        render();
    }

    /**
     * Toggles debug information display
     */
    public void toggleDebugInfo() {
        this.showDebugInfo = !this.showDebugInfo;
        render(); // Refresh display
    }

    /**
     * Saves current game state
     * 
     * @param config Game configuration to save to
     */
    public void saveGameState(GameConfig config) {
        // Save field dimensions
        config.setFieldWidth(fieldManager.getWidth());
        config.setFieldHeight(fieldManager.getHeight());

        // Save robots
        for (TeamRobot robot : gameController.getRedTeam()) {
            config.addRobot(new GameConfig.RobotConfig(
                    robot.getX(), robot.getY(), true,
                    robot.getCurrentRole(), robot.getSensorRange(),
                    robot.getMaxSpeed()));
        }

        for (TeamRobot robot : gameController.getBlueTeam()) {
            config.addRobot(new GameConfig.RobotConfig(
                    robot.getX(), robot.getY(), false,
                    robot.getCurrentRole(), robot.getSensorRange(),
                    robot.getMaxSpeed()));
        }

        // Save obstacles
        for (AbstractArenaObject obstacle : gameController.getObstacles()) {
            if (obstacle instanceof Obstacle) {
                Obstacle obs = (Obstacle) obstacle;
                config.addObstacle(new GameConfig.ObstacleConfig(
                        obs.getX(), obs.getY(),
                        obs.getWidth(), obs.getHeight(),
                        obs.getType()));
            }
        }

        // Save ball state
        Ball ball = gameController.getBall();
        config.setBall(new GameConfig.BallConfig(
                ball.getX(), ball.getY(),
                ball.getDX(), ball.getDY()));
    }

    /**
     * Loads game state from configuration
     * 
     * @param config Game configuration to load from
     */
    public void loadGameState(GameConfig config) {
        gameController.loadConfig(config);
        fieldManager = new FieldManager(
                config.getFieldWidth(),
                config.getFieldHeight(),
                20, 60);
        render();
    }
}