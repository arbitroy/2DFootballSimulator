package com.footballsim.controllers;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import com.footballsim.models.*;
import com.footballsim.utils.PhysicsEngine;


/**
 * Controls the game state and coordinates all game components.
 */
public class GameController {
    // Game state - protected by state lock
    private final ReentrantLock stateLock = new ReentrantLock();
    private volatile boolean isRunning;
    private volatile int matchTimeSeconds;
    private volatile int redScore;
    private volatile int blueScore;
    private volatile double gameSpeed;

    // Field properties - immutable after construction
    private double fieldWidth;
    private double fieldHeight;
    private double borderWidth;
    private double goalWidth;

    // Game objects - protected by collections lock
    private final ReentrantLock collectionsLock = new ReentrantLock();
    private final Ball ball;
    private final List<TeamRobot> redTeam;
    private final List<TeamRobot> blueTeam;
    private final List<AbstractArenaObject> obstacles;

    private List<String> matchHistory = new ArrayList<>();
    private final PhysicsEngine physicsEngine;

    // Thread-safe event listeners
    private final CopyOnWriteArrayList<GameEventListener> eventListeners;

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
        this(600, 400); // Default field dimensions
    }

    /**
     * Creates a new game controller with specified field dimensions
     * 
     * @param fieldWidth  Width of the playing field
     * @param fieldHeight Height of the playing field
     */
    public GameController(double fieldWidth, double fieldHeight) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.borderWidth = 20;
        this.goalWidth = 60;

        // Initialize collections with thread-safe implementations
        this.redTeam = Collections.synchronizedList(new ArrayList<>());
        this.blueTeam = Collections.synchronizedList(new ArrayList<>());
        this.obstacles = Collections.synchronizedList(new ArrayList<>());
        this.eventListeners = new CopyOnWriteArrayList<>();

        // Create ball at center of field
        this.ball = new Ball(fieldWidth / 2 + borderWidth, fieldHeight / 2 + borderWidth);
        this.physicsEngine = new PhysicsEngine();

        initializeGame();
    }

    /**
     * Initializes or resets the game state
     */
    private void initializeGame() {
        stateLock.lock();
        try {
            isRunning = false;
            matchTimeSeconds = 500; // 5 minutes default
            gameSpeed = 1.0;
            redScore = 0;
            blueScore = 0;

            collectionsLock.lock();
            try {
                // Clear all collections
                redTeam.clear();
                blueTeam.clear();
                obstacles.clear();

                // Reset ball to center
                synchronized (ball) {
                    ball.setPosition(fieldWidth / 2 + borderWidth, fieldHeight / 2 + borderWidth);
                    ball.setVelocity(0, 0);
                }
            } finally {
                collectionsLock.unlock();
            }
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Updates game state for current frame
     */
    public void update() {
        if (!isRunning)
            return;

        stateLock.lock();
        try {
            collectionsLock.lock();
            try {
                // Update match time based on game speed
                if (matchTimeSeconds > 0) {
                    matchTimeSeconds -= gameSpeed / 60; // Divide by 60 for ~60 FPS
                    if (matchTimeSeconds <= 0) {
                        matchTimeSeconds = 0;
                        endGame();
                    }
                    notifyTimeUpdated();
                }

                // Update ball with current field dimensions
                ball.update(fieldWidth, fieldHeight, borderWidth);

                // Get all robots for collision handling
                List<TeamRobot> allRobots = new ArrayList<>(redTeam);
                allRobots.addAll(blueTeam);

                // Update physics for all game objects
                physicsEngine.update(ball, allRobots, obstacles, fieldWidth, fieldHeight);

                // Update robot behaviors and keep within boundaries
                for (TeamRobot robot : allRobots) {
                    // Keep robots within field boundaries
                    double x = robot.getX();
                    double y = robot.getY();

                    x = Math.max(borderWidth, Math.min(x, borderWidth + fieldWidth - robot.getWidth()));
                    y = Math.max(borderWidth, Math.min(y, borderWidth + fieldHeight - robot.getHeight()));

                    robot.setPosition(x, y);

                    // Update robot behaviors
                    if (robot.isRedTeam()) {
                        robot.updateBehavior(ball, obstacles, redTeam, blueTeam);
                    } else {
                        robot.updateBehavior(ball, obstacles, blueTeam, redTeam);
                    }
                }

                // Check for goals
                checkForGoals();
            } finally {
                collectionsLock.unlock();
            }
        } finally {
            stateLock.unlock();
        }
    }


    /**
     * Checks if a goal has been scored
     */
    private void checkForGoals() {
        synchronized (ball) {
            double ballX = ball.getX();
            double ballY = ball.getY();
            double goalTop = (fieldHeight - goalWidth) / 2 + borderWidth;
            double goalBottom = goalTop + goalWidth;

            if (ballX <= borderWidth && ballY >= goalTop && ballY <= goalBottom) {
                redScore++;
                recordGoal(true);
                notifyGoalScored(true);
                resetAfterGoal();
            } else if (ballX >= fieldWidth + borderWidth && ballY >= goalTop && ballY <= goalBottom) {
                blueScore++;
                recordGoal(false);
                notifyGoalScored(false);
                resetAfterGoal();
            }
        }
    }

    /**
     * Records a goal event in match history
     * 
     * @param isRedTeam true if red team scored
     */
    private void recordGoal(boolean isRedTeam) {
        String timeStamp = String.format("%02d:%02d", matchTimeSeconds / 60, matchTimeSeconds % 60);
        String team = isRedTeam ? "Red" : "Blue";
        matchHistory.add(String.format("[%s] GOAL! %s team scores! Score: %d-%d",
                timeStamp, team, redScore, blueScore));
    }

    /**
     * Gets the match history
     * 
     * @return List of match events
     */
    public List<String> getMatchHistory() {
        return Collections.unmodifiableList(matchHistory);
    }

    /**
     *
     * @param width
     * @param height
     */
    public void updateFieldDimensions(double width, double height) {
        stateLock.lock();
        try {
            this.fieldWidth = width;
            this.fieldHeight = height;

            // Reset ball position to new center
            synchronized (ball) {
                ball.setPosition(width / 2 + borderWidth, height / 2 + borderWidth);
                ball.setVelocity(0, 0);
            }
        } finally {
            stateLock.unlock();
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
                matchTimeSeconds = 0; // Ensure we don't go negative
                endGame();
            }
        }
    }

    /**
     * Sets the match duration in seconds
     * 
     * @param minutes Match duration in seconds
     * @throws IllegalArgumentException if duration is less than 1 minute or more
     *                                  than 30 minutes
     */
    public void setMatchDuration(int minutes) {
        if (minutes < 1 || minutes > 30) {
            throw new IllegalArgumentException("Match duration must be between 1 and 30 minutes");
        }
        this.matchTimeSeconds = minutes * 60;
    }

    /**
     * Resets positions after a goal
     */
    private void resetAfterGoal() {
        // Reset ball to center
        ball.setPosition(fieldWidth / 2 + borderWidth, fieldHeight / 2 + borderWidth);
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
        // Create a copy of the list to avoid concurrent modification
        List<GameEventListener> listenersCopy;
        synchronized (eventListeners) {
            listenersCopy = new ArrayList<>(eventListeners);
        }
        for (GameEventListener listener : listenersCopy) {
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
        collectionsLock.lock();
        try {
            if (robot.isRedTeam()) {
                redTeam.add(robot);
            } else {
                blueTeam.add(robot);
            }
        } finally {
            collectionsLock.unlock();
        }
    }

    /**
     * Removes a robot from either team
     * 
     * @param robot Robot to remove
     */
    public void removeRobot(TeamRobot robot) {
        collectionsLock.lock();
        try {
            if (robot.isRedTeam()) {
                redTeam.remove(robot);
            } else {
                blueTeam.remove(robot);
            }
        } finally {
            collectionsLock.unlock();
        }
    }

    /**
     * Gets the total width including borders
     * 
     * @return Total width
     */
    public double getTotalWidth() {
        return fieldWidth + (borderWidth * 2);
    }

    /**
     * Gets the total height including borders
     * 
     * @return Total height
     */
    public double getTotalHeight() {
        return fieldHeight + (borderWidth * 2);
    }

    /**
     * Gets the width of the border
     * 
     * @return Border width
     */
    public double getBorderWidth() {
        return borderWidth;
    }

    /**
     * Gets the width of the goals
     * 
     * @return Goal width
     */
    public double getGoalWidth() {
        return goalWidth;
    }

    /**
     * Gets the current match time in seconds
     * 
     * @return Match time in seconds
     */
    public int getMatchTimeSeconds() {
        return matchTimeSeconds;
    }

    /**
     * Sets the match duration in seconds
     * 
     * @param seconds New match duration
     */
    public void setMatchTimeSeconds(int seconds) {
        this.matchTimeSeconds = seconds;
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
        stateLock.lock();
        try {
            collectionsLock.lock();
            try {
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

                Ball newBall = config.getBall().createBall();
                synchronized (ball) {
                    ball.setPosition(newBall.getX(), newBall.getY());
                    ball.setVelocity(newBall.getDX(), newBall.getDY());
                }

                matchTimeSeconds = config.getMatchDuration();
                gameSpeed = config.getGameSpeed();
            } finally {
                collectionsLock.unlock();
            }
        } finally {
            stateLock.unlock();
        }
    }

    // Event listener management
    public void addEventListener(GameEventListener listener) {
        eventListeners.add(listener);
    }

    public void removeEventListener(GameEventListener listener) {
        eventListeners.remove(listener);
    }

    // Getters
    public double getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(double speed) {
        this.gameSpeed = speed;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getRedScore() {
        return redScore;
    }

    public int getBlueScore() {
        return blueScore;
    }

    public Ball getBall() {
        return ball;
    }

    public List<TeamRobot> getRedTeam() {
        collectionsLock.lock();
        try {
            return new ArrayList<>(redTeam);
        } finally {
            collectionsLock.unlock();
        }
    }

    public List<TeamRobot> getBlueTeam() {
        collectionsLock.lock();
        try {
            return new ArrayList<>(blueTeam);
        } finally {
            collectionsLock.unlock();
        }
    }

    public List<AbstractArenaObject> getObstacles() {
        collectionsLock.lock();
        try {
            return new ArrayList<>(obstacles);
        } finally {
            collectionsLock.unlock();
        }
    }
}