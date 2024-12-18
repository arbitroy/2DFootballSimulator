package com.footballsim.controllers;

import javafx.animation.AnimationTimer;

/**
 * Manages game updates and rendering in separate threads
 * to prevent UI blocking
 */
public class GameLoop {
    private final GameController gameController;
    private final GameCoordinator coordinator;
    private AnimationTimer gameTimer;
    private volatile boolean isRunning;

    public GameLoop(GameController gameController, GameCoordinator coordinator) {
        this.gameController = gameController;
        this.coordinator = coordinator;
        setupGameLoop();
    }

    private void setupGameLoop() {
        gameTimer = new AnimationTimer() {
            private long previousTime = 0;
            private static final long INTERVAL = 16_000_000; // 16ms in nanoseconds

            @Override
            public void handle(long currentTime) {
                if (previousTime == 0) {
                    previousTime = currentTime;
                    return;
                }

                if (isRunning) {
                    long elapsedTime = currentTime - previousTime;

                    if (elapsedTime >= INTERVAL) {
                        try {
                            gameController.update();
                            coordinator.render();
                            previousTime = currentTime;
                        } catch (Exception e) {
                            System.err.println("Game loop error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    public void start() {
        isRunning = true;
        gameTimer.start();
    }

    public void stop() {
        isRunning = false;
        gameTimer.stop();
    }

    public void shutdown() {
        stop();
    }
}