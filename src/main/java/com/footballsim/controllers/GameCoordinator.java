package com.footballsim.controllers;

import com.footballsim.models.*;
import com.footballsim.utils.DebugVisualizer;
import com.footballsim.utils.Line;
import com.footballsim.views.*;
import com.footballsim.formations.FormationManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

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
    private final GameLoop gameLoop;
    private boolean showDebugInfo = false;
    private boolean gameRunning = false;

    private final ReentrantLock updateLock = new ReentrantLock();

    /**
     * Creates a new game coordinator with the specified components
     */
    public GameCoordinator(ArenaCanvas arenaCanvas, GameSettingsPanel settingsPanel,
            RobotControlPanel robotPanel) {
        this.arenaCanvas = arenaCanvas;
        this.settingsPanel = settingsPanel;
        this.robotPanel = robotPanel;

        // Initialize core components
        this.fieldManager = new FieldManager(arenaCanvas.getFieldWidth(),
                arenaCanvas.getFieldHeight(), 20, 60);
                this.gameController = new GameController(
                    arenaCanvas.getFieldWidth(),
                    arenaCanvas.getFieldHeight()
                );
        this.gameLoop = new GameLoop(gameController, this);
        this.redTeamFormation = new FormationManager(fieldManager, true);
        this.blueTeamFormation = new FormationManager(fieldManager, false);

        setupEventHandlers();

        // Update UI components on JavaFX thread
        gameController.addEventListener(new GameController.GameEventListener() {
            @Override
            public void onGoalScored(boolean redTeam) {
                Platform.runLater(() -> {
                    settingsPanel.updateScore(
                        gameController.getRedScore(),
                        gameController.getBlueScore()
                    );
                });
            }
    
            @Override
            public void onTimeUpdated(int minutes, int seconds) {
                Platform.runLater(() -> {
                    settingsPanel.updateTimer(minutes, seconds);
                });
            }
    
            @Override
            public void onGameEnd(boolean redTeamWon) {
                Platform.runLater(() -> {
                    stopGame();
                    showGameEndDialog(redTeamWon);
                });
            }
        });

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
        double borderWidth = arenaCanvas.getBorderWidth();
        double fieldWidth = fieldManager.getWidth();
        double fieldHeight = fieldManager.getHeight();

        // Clear entire canvas
        clearCanvas(gc);

        // Draw border
        gc.setFill(BORDER_COLOR);
        gc.fillRect(0, 0, fieldWidth + 2 * borderWidth, fieldHeight + 2 * borderWidth);

        // Draw field
        gc.setFill(FIELD_COLOR);
        gc.fillRect(borderWidth, borderWidth, fieldWidth, fieldHeight);

        // Draw center line - use field manager for positions
        gc.setStroke(LINE_COLOR);
        gc.setLineWidth(LINE_WIDTH);
        double centerX = borderWidth + fieldWidth/2;
        gc.strokeLine(centerX, borderWidth, centerX, borderWidth + fieldHeight);

        // Draw center circle - scale radius based on field size
        double centerY = borderWidth + fieldHeight/2;
        double circleRadius = Math.min(fieldWidth, fieldHeight) * 0.15; // Scale with field size
        gc.strokeOval(centerX - circleRadius, centerY - circleRadius,
                circleRadius * 2, circleRadius * 2);

        // Draw penalty areas - scale with field size
        double penaltyAreaWidth = fieldWidth * 0.15;
        double penaltyAreaHeight = fieldHeight * 0.4;
        double penaltyAreaY = borderWidth + (fieldHeight - penaltyAreaHeight)/2;

        // Left penalty area
        gc.strokeRect(borderWidth, penaltyAreaY, penaltyAreaWidth, penaltyAreaHeight);

        // Right penalty area
        gc.strokeRect(borderWidth + fieldWidth - penaltyAreaWidth, penaltyAreaY,
                penaltyAreaWidth, penaltyAreaHeight);

        // Draw goals - scale with field size
        double goalWidth = Math.min(fieldHeight * 0.15, 60); // Scale but cap at 60
        double goalDepth = Math.min(fieldWidth * 0.05, 20);  // Scale but cap at 20
        drawGoal(gc, true, goalWidth, goalDepth);  // Left goal
        drawGoal(gc, false, goalWidth, goalDepth); // Right goal
    }

    private void drawGoal(GraphicsContext gc, boolean isLeft, double goalWidth, double goalDepth) {
        gc.setStroke(LINE_COLOR);
        gc.setLineWidth(LINE_WIDTH * 2);

        double borderWidth = arenaCanvas.getBorderWidth();
        double fieldHeight = fieldManager.getHeight();
        double x = isLeft ? borderWidth : borderWidth + fieldManager.getWidth();
        double y = borderWidth + (fieldHeight - goalWidth) / 2;

        if (isLeft) {
            gc.strokeLine(x, y, x - goalDepth, y);               // Top
            gc.strokeLine(x - goalDepth, y, x - goalDepth, y + goalWidth); // Back
            gc.strokeLine(x - goalDepth, y + goalWidth, x, y + goalWidth); // Bottom
        } else {
            gc.strokeLine(x, y, x + goalDepth, y);               // Top
            gc.strokeLine(x + goalDepth, y, x + goalDepth, y + goalWidth); // Back
            gc.strokeLine(x + goalDepth, y + goalWidth, x, y + goalWidth); // Bottom
        }

        gc.setLineWidth(LINE_WIDTH);
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
        // Remove existing handlers first
        arenaCanvas.setOnMousePressed(null);
        arenaCanvas.setOnMouseDragged(null);
        arenaCanvas.setOnMouseReleased(null);

        // Add new handlers
        arenaCanvas.setOnMousePressed(this::handleMousePressed);
        arenaCanvas.setOnMouseDragged(this::handleMouseDragged);
        arenaCanvas.setOnMouseReleased(this::handleMouseReleased);

        // Make sure canvas can receive input
        arenaCanvas.setFocusTraversable(true);
    }


    /**
     * Renders the game state - called on JavaFX thread
     */
    public void render() {
        try {
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(this::render);
                return;
            }

            GraphicsContext gc = arenaCanvas.getGraphicsContext2D();
            clearCanvas(gc);
            drawField(gc);

            // Batch robot updates to minimize garbage collection
            List<TeamRobot> redTeam = gameController.getRedTeam();
            List<TeamRobot> blueTeam = gameController.getBlueTeam();

            if (redTeam != null && blueTeam != null) {
                drawBall(gc);
                drawRobots(gc);
                drawObstacles(gc);

                if (showDebugInfo) {
                    Ball ball = gameController.getBall();
                    if (ball != null) {
                        DebugVisualizer.drawBallDebug(gc, ball);
                    }

                    for (TeamRobot robot : redTeam) {
                        if (robot != null) {
                            DebugVisualizer.drawRobotDebug(gc, robot);
                        }
                    }
                    for (TeamRobot robot : blueTeam) {
                        if (robot != null) {
                            DebugVisualizer.drawRobotDebug(gc, robot);
                        }
                    }

                    for (AbstractArenaObject obj : gameController.getObstacles()) {
                        if (obj instanceof Obstacle) {
                            DebugVisualizer.drawObstacleDebug(gc, (Obstacle)obj);
                        }
                    }
                }

                if (selectedRobot != null) {
                    drawSelectionIndicator(gc, selectedRobot);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in render: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void startGame() {
        updateLock.lock();
        try {
            if (!gameRunning) {
                gameController.startGame();
                gameLoop.start();
                gameRunning = true;
            }
        } finally {
            updateLock.unlock();
        }
    }

    public void stopGame() {
        updateLock.lock();
        try {
            if (gameRunning) {
                gameController.pauseGame();
                gameLoop.stop();
                gameRunning = false;
            }
        } finally {
            updateLock.unlock();
        }
    }

    public void resetGame() {
        updateLock.lock();
        try {
            // Stop game if running
            if (gameRunning) {
                stopGame();
            }

            // Reset game state through game controller
            gameController.resetGame();

            // Reset teams to default positions
            setupPlayerTeam();
            setupOpposingTeam();

            // Update UI
            robotPanel.updateRobotCounts(
                    getRedTeamCount(),
                    getBlueTeamCount()
            );

            // Render the reset state
            render();
        } finally {
            updateLock.unlock();
        }
    }
    /**
     * Sets up basic opposing team
     */
    public void setupOpposingTeam() {
        // Get field dimensions for positioning
        double fieldStartX = fieldManager.getBoundaries()[0].getXY()[0];
        double fieldStartY = fieldManager.getBoundaries()[0].getXY()[1];
        double fieldWidth = fieldManager.getWidth();
        double fieldHeight = fieldManager.getHeight();

        // Create basic formation - 1 goalkeeper, 2 defenders, 2 attackers
        double[][] positions = {
                {fieldWidth * 0.9, fieldHeight * 0.5},     // Goalkeeper
                {fieldWidth * 0.7, fieldHeight * 0.3},     // Defender 1
                {fieldWidth * 0.7, fieldHeight * 0.7},     // Defender 2
                {fieldWidth * 0.6, fieldHeight * 0.4},     // Attacker 1
                {fieldWidth * 0.6, fieldHeight * 0.6}      // Attacker 2
        };

        TeamRobot.RobotRole[] roles = {
                TeamRobot.RobotRole.GOALKEEPER,
                TeamRobot.RobotRole.DEFENDER,
                TeamRobot.RobotRole.DEFENDER,
                TeamRobot.RobotRole.ATTACKER,
                TeamRobot.RobotRole.ATTACKER
        };

        // Create and add opposing team robots
        for (int i = 0; i < positions.length; i++) {
            TeamRobot robot = new TeamRobot(
                    fieldStartX + positions[i][0],
                    fieldStartY + positions[i][1],
                    false  // Blue team
            );
            robot.setRole(roles[i]);
            gameController.addRobot(robot);
        }
    }

    /**
     * Sets up initial player team
     */
    public void setupPlayerTeam() {
        // Mirror positions for player team
        double fieldStartX = fieldManager.getBoundaries()[0].getXY()[0];
        double fieldStartY = fieldManager.getBoundaries()[0].getXY()[1];
        double fieldWidth = fieldManager.getWidth();
        double fieldHeight = fieldManager.getHeight();

        double[][] positions = {
                {fieldWidth * 0.1, fieldHeight * 0.5},     // Goalkeeper
                {fieldWidth * 0.3, fieldHeight * 0.3},     // Defender 1
                {fieldWidth * 0.3, fieldHeight * 0.7},     // Defender 2
                {fieldWidth * 0.4, fieldHeight * 0.4},     // Attacker 1
                {fieldWidth * 0.4, fieldHeight * 0.6}      // Attacker 2
        };

        TeamRobot.RobotRole[] roles = {
                TeamRobot.RobotRole.GOALKEEPER,
                TeamRobot.RobotRole.DEFENDER,
                TeamRobot.RobotRole.DEFENDER,
                TeamRobot.RobotRole.ATTACKER,
                TeamRobot.RobotRole.ATTACKER
        };

        // Create and add player team robots
        for (int i = 0; i < positions.length; i++) {
            TeamRobot robot = new TeamRobot(
                    fieldStartX + positions[i][0],
                    fieldStartY + positions[i][1],
                    true  // Red team
            );
            robot.setRole(roles[i]);
            gameController.addRobot(robot);
        }
    }

    public void cleanup() {
        gameLoop.shutdown();
    }

    /**
     * Updates game state
     */
    public void update() {
        updateLock.lock();
        try {
            if (gameController.isRunning()) {
                gameController.update();
                updateFormations();

                // Debug ball state
                Ball ball = gameController.getBall();
                if (ball != null && (ball.getDX() != 0 || ball.getDY() != 0)) {
                    System.out.println("Ball velocity: " + ball.getDX() + ", " + ball.getDY());
                }

                render();
            }
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * Updates team formations
     */
    private void updateFormations() {
        redTeamFormation.assignPositions(gameController.getRedTeam());
        blueTeamFormation.assignPositions(gameController.getBlueTeam());
    }

    /**
     * Handles mouse press events
     * 
     * @param event Mouse event
     */
    private void handleMousePressed(MouseEvent event) {
        if (!gameRunning) return;

        double x = event.getX();
        double y = event.getY();

        // Get field boundaries
        double borderWidth = arenaCanvas.getBorderWidth();
        double fieldWidth = fieldManager.getWidth();
        double fieldHeight = fieldManager.getHeight();

        // Check if click is within playable area
        if (x < borderWidth || x > borderWidth + fieldWidth ||
                y < borderWidth || y > borderWidth + fieldHeight) {
            return;
        }

        // Check for robot selection first
        TeamRobot clickedRobot = findRobotAt(x, y);
        if (clickedRobot != null) {
            selectedRobot = clickedRobot;
            isDraggingRobot = true;
            dragStartX = x - clickedRobot.getX();
            dragStartY = y - clickedRobot.getY();
            return;
        }

        // Handle ball kick
        Ball ball = gameController.getBall();
        if (ball != null) {
            // Calculate distance from click to ball
            double dx = x - ball.getX();
            double dy = y - ball.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);

            // Kick if clicked near the ball
            if (dist < 50) {
                // Normalize direction and apply force
                dx = dx / dist;
                dy = dy / dist;
                double kickPower = 5.0;
                ball.applyForce(dx * kickPower, dy * kickPower);
            }
        }
    }



    /**
     * Handles mouse drag events
     * 
     * @param event Mouse event
     */
    private void handleMouseDragged(MouseEvent event) {
        if (isDraggingRobot && selectedRobot != null) {
            double borderWidth = arenaCanvas.getBorderWidth();
            double fieldWidth = fieldManager.getWidth();
            double fieldHeight = fieldManager.getHeight();

            // Calculate new position
            double newX = event.getX() - dragStartX;
            double newY = event.getY() - dragStartY;

            // Keep robot within playable area
            newX = Math.max(borderWidth, Math.min(newX, borderWidth + fieldWidth - selectedRobot.getWidth()));
            newY = Math.max(borderWidth, Math.min(newY, borderWidth + fieldHeight - selectedRobot.getHeight()));

            selectedRobot.setPosition(newX, newY);
            render();
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
     * Updates the field dimensions
     * 
     * @param width  New field width
     * @param height New field height
     */
    public void updateFieldDimensions(Double width, Double height) {
        updateLock.lock();
        try {
            // Validate dimensions
            final double validWidth = Math.max(300, Math.min(width, 800));
            final double validHeight = Math.max(200, Math.min(height, 600));

            // Stop game temporarily if running
            boolean wasRunning = gameRunning;
            if (wasRunning) {
                stopGame();
            }

            // Update field dimensions in all components
            arenaCanvas.setFieldDimensions(validWidth, validHeight);
            this.fieldManager = new FieldManager(validWidth, validHeight, arenaCanvas.getBorderWidth(), 60);
            gameController.updateFieldDimensions(validWidth, validHeight);

            // Reset teams to fit new dimensions
            if (gameController.getRedTeam().size() > 0 || gameController.getBlueTeam().size() > 0) {
                setupPlayerTeam();
                setupOpposingTeam();
            }

            // Restart game if it was running
            if (wasRunning) {
                startGame();
            }

            render();
        } finally {
            updateLock.unlock();
        }
    }

    public void addRobot(boolean isRedTeam, TeamRobot.RobotRole role) {
        // Calculate default position based on team
        double x = isRedTeam ? 
            fieldManager.getBoundaries()[0].getXY()[0] + fieldManager.getWidth() * 0.25 : // Red team left side
            fieldManager.getBoundaries()[0].getXY()[0] + fieldManager.getWidth() * 0.75;  // Blue team right side
        double y = fieldManager.getBoundaries()[0].getXY()[1] + fieldManager.getHeight() / 2;
    
        // Create and configure new robot
        TeamRobot robot = new TeamRobot(x, y, isRedTeam);
        robot.setRole(role);
    
        // Add to game controller
        gameController.addRobot(robot);
    
        // Update formations
        if (isRedTeam) {
            redTeamFormation.assignPositions(gameController.getRedTeam());
        } else {
            blueTeamFormation.assignPositions(gameController.getBlueTeam());
        }
    }
    
    public void removeRobot(int index) {
        List<TeamRobot> redTeam = gameController.getRedTeam();
        List<TeamRobot> blueTeam = gameController.getBlueTeam();
        
        if (index < redTeam.size()) {
            // Remove from red team
            TeamRobot robot = redTeam.get(index);
            gameController.removeRobot(robot);
            redTeamFormation.removeRobot(robot);
        } else {
            // Remove from blue team
            index -= redTeam.size();
            if (index < blueTeam.size()) {
                TeamRobot robot = blueTeam.get(index);
                gameController.removeRobot(robot);
                blueTeamFormation.removeRobot(robot);
            }
        }
    }
    
    public int getRedTeamCount() {
        return gameController.getRedTeam().size();
    }
    
    public int getBlueTeamCount() {
        return gameController.getBlueTeam().size();
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