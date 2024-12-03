package com.footballsim.controllers;

import java.util.*;

import com.footballsim.models.AbstractArenaObject;
import com.footballsim.models.Ball;
import com.footballsim.models.GameConfig;
import com.footballsim.models.TeamRobot;
import com.footballsim.utils.PhysicsEngine;

/**
 * Controls the game state and coordinates all game components.
 */
public class GameController {
    // Game state
    private boolean isRunning;
    private int matchTimeSeconds;
    private int redScore;
    private int blueScore;
    private double gameSpeed;

    // Field properties
    private double fieldWidth;
    private double fieldHeight;
    private double borderWidth;
    private double goalWidth;

    // Game objects
    private Ball ball;
    private List<TeamRobot> redTeam;
    private List<TeamRobot> blueTeam;
    private List<AbstractArenaObject> obstacles;
    private PhysicsEngine physicsEngine;

    // Event listeners
    private List<GameEventListener> eventListeners;

    /**
     * Interface for game event notifications
     */
    public interface GameEventListener {
        void onGoalScored(boolean redTeam);
        void onTimeUpdated(int minutes, int seconds);
        void onGameEnd(boolean redTeamWon);
    }

    /**
     * Creates a new game controller with default settings
     */
    public GameController() {
        this.fieldWidth = 600;
        this.fieldHeight = 400;
        this.borderWidth = 20;
        this.goalWidth = 60;
        
        initializeGame();
    }

    /**
     * Creates a new game controller with specified field dimensions
     * @param fieldWidth Width of the playing field
     * @param fieldHeight Height of the playing field
     */
    public GameController(double fieldWidth, double fieldHeight) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.borderWidth = 20;
        this.goalWidth = 60;
        
        initializeGame();
    }

    /**
     * Initializes game objects and state
     */
    private void initializeGame() {
        isRunning = false;
        matchTimeSeconds = 300; // 5 minutes default
        gameSpeed = 1.0;
        redScore = 0;
        blueScore = 0;

        // Initialize collections
        redTeam = new ArrayList<>();
        blueTeam = new ArrayList<>();
        obstacles = new ArrayList<>();
        eventListeners = new ArrayList<>();

        // Create ball at center of field
        ball = new Ball(fieldWidth/2 + borderWidth, fieldHeight/2 + borderWidth);

        // Initialize physics engine
        physicsEngine = new PhysicsEngine();
    }

    /**
     * Updates game state for current frame
     */
    public void update() {
        if (!isRunning) return;

        // Update physics
        List<TeamRobot> allRobots = new ArrayList<>(redTeam);
        allRobots.addAll(blueTeam);
        physicsEngine.update(ball, allRobots, obstacles, fieldWidth, fieldHeight);

        // Update robots
        updateTeam(redTeam, blueTeam);
        updateTeam(blueTeam, redTeam);

        // Check for goals
        checkForGoals();

        // Update match time
        updateMatchTime();
    }

    /**
     * Updates robots in a team
     * @param team Team to update
     * @param opponents Opposing team
     */
    private void updateTeam(List<TeamRobot> team, List<TeamRobot> opponents) {
        for (TeamRobot robot : team) {
            robot.updateBehavior(ball, obstacles, team, opponents);
        }
    }

    /**
     * Checks if a goal has been scored
     */
    private void checkForGoals() {
        double ballX = ball.getX();
        double ballY = ball.getY();
        double goalTop = (fieldHeight - goalWidth)/2 + borderWidth;
        double goalBottom = goalTop + goalWidth;

        // Check left goal (blue team)
        if (ballX <= borderWidth && ballY >= goalTop && ballY <= goalBottom) {
            redScore++;
            notifyGoalScored(true);
            resetAfterGoal();
        }
        // Check right goal (red team)
        else if (ballX >= fieldWidth + borderWidth && ballY >= goalTop && ballY <= goalBottom) {
            blueScore++;
            notifyGoalScored(false);
            resetAfterGoal();
        }
    }

    /**
     * Updates match time and checks for game end
     */
    private void updateMatchTime() {
        if (matchTimeSeconds > 0) {
            matchTimeSeconds -= gameSpeed;
            notifyTimeUpdated();

            if (matchTimeSeconds <= 0) {
                endGame();
            }
        }
    }

    /**
     * Resets positions after a goal
     */
    private void resetAfterGoal() {
        // Reset ball to center
        ball.setPosition(fieldWidth/2 + borderWidth, fieldHeight/2 + borderWidth);
        ball.setVelocity(0, 0);

        // Could add robot reset positions here
    }

    /**
     * Ends the game and determines winner
     */
    private void endGame() {
        isRunning = false;
        boolean redTeamWon = redScore > blueScore;
        notifyGameEnd(redTeamWon);
    }

    // Event notification methods
    private void notifyGoalScored(boolean redTeam) {
        for (GameEventListener listener : eventListeners) {
            listener.onGoalScored(redTeam);
        }
    }

    private void notifyTimeUpdated() {
        int minutes = matchTimeSeconds / 60;
        int seconds = matchTimeSeconds % 60;
        for (GameEventListener listener : eventListeners) {
            listener.onTimeUpdated(minutes, seconds);
        }
    }

    private void notifyGameEnd(boolean redTeamWon) {
        for (GameEventListener listener : eventListeners) {
            listener.onGameEnd(redTeamWon);
        }
    }

    // Game control methods
    public void startGame() {
        isRunning = true;
    }

    public void pauseGame() {
        isRunning = false;
    }

    public void resetGame() {
        initializeGame();
    }

    // Robot management
    public void addRobot(TeamRobot robot) {
        if (robot.isRedTeam()) {
            redTeam.add(robot);
        } else {
            blueTeam.add(robot);
        }
    }

    public void removeRobot(TeamRobot robot) {
        if (robot.isRedTeam()) {
            redTeam.remove(robot);
        } else {
            blueTeam.remove(robot);
        }
    }

    // Obstacle management
    public void addObstacle(AbstractArenaObject obstacle) {
        obstacles.add(obstacle);
    }

    public void removeObstacle(AbstractArenaObject obstacle) {
        obstacles.remove(obstacle);
    }

    // Configuration methods
    public void loadConfig(GameConfig config) {
        fieldWidth = config.getFieldWidth();
        fieldHeight = config.getFieldHeight();
        
        // Clear existing objects
        redTeam.clear();
        blueTeam.clear();
        obstacles.clear();

        // Create new objects from config
        for (GameConfig.RobotConfig robotConfig : config.getRobots()) {
            TeamRobot robot = robotConfig.createRobot();
            addRobot(robot);
        }

        for (GameConfig.ObstacleConfig obstacleConfig : config.getObstacles()) {
            addObstacle(obstacleConfig.createObstacle());
        }

        ball = config.getBall().createBall();
        matchTimeSeconds = config.getMatchDuration();
        gameSpeed = config.getGameSpeed();
    }

    // Event listener management
    public void addEventListener(GameEventListener listener) {
        eventListeners.add(listener);
    }

    public void removeEventListener(GameEventListener listener) {
        eventListeners.remove(listener);
    }

    // Getters
    public Ball getBall() { return ball; }
    public List<TeamRobot> getRedTeam() { return Collections.unmodifiableList(redTeam); }
    public List<TeamRobot> getBlueTeam() { return Collections.unmodifiableList(blueTeam); }
    public List<AbstractArenaObject> getObstacles() { return Collections.unmodifiableList(obstacles); }
    public int getRedScore() { return redScore; }
    public int getBlueScore() { return blueScore; }
    public int getMatchTimeSeconds() { return matchTimeSeconds; }
    public boolean isRunning() { return isRunning; }
    public double getGameSpeed() { return gameSpeed; }
    public void setGameSpeed(double speed) { this.gameSpeed = speed; }
}