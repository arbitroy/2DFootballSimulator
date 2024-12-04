package com.footballsim.controllers;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import java.util.concurrent.*;

/**
 * Manages game updates and rendering in separate threads
 * to prevent UI blocking
 */
public class GameLoop {
    private final GameController gameController;
    private final GameCoordinator coordinator;
    private final ExecutorService physicsExecutor;
    private final ScheduledExecutorService updateExecutor;
    private AnimationTimer renderTimer;
    private volatile boolean isRunning;
    private static final long PHYSICS_UPDATE_RATE = 16_666_667L; // 60 FPS in nanoseconds

    public GameLoop(GameController gameController, GameCoordinator coordinator) {
        this.gameController = gameController;
        this.coordinator = coordinator;
        this.physicsExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("Physics-Thread");
            thread.setDaemon(true);
            return thread;
        });
        this.updateExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("Game-Update-Thread");
            thread.setDaemon(true);
            return thread;
        });
        setupGameLoop();
    }

    /**
     * Sets up the game loop with separate threads for physics,
     * game state updates, and rendering
     */
    private void setupGameLoop() {
        // Create render timer that runs on JavaFX thread
        renderTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                // Render on JavaFX thread
                coordinator.render();
                lastUpdate = now;
            }
        };

        // Schedule physics updates at fixed rate
        updateExecutor.scheduleAtFixedRate(() -> {
            if (isRunning) {
                try {
                    // Submit physics calculations to physics thread
                    Future<?> physicsFuture = physicsExecutor.submit(() -> {
                        gameController.update();
                    });

                    // Wait for physics to complete with timeout
                    physicsFuture.get(16, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    // Log error but continue running
                    System.err.println("Physics update error: " + e.getMessage());
                }
            }
        }, 0, PHYSICS_UPDATE_RATE, TimeUnit.NANOSECONDS);
    }

    /**
     * Starts the game loop
     */
    public void start() {
        isRunning = true;
        renderTimer.start();
    }

    /**
     * Stops the game loop
     */
    public void stop() {
        isRunning = false;
        renderTimer.stop();
    }

    /**
     * Cleans up resources when the game is closing
     */
    public void shutdown() {
        stop();
        physicsExecutor.shutdown();
        updateExecutor.shutdown();
        try {
            // Wait for threads to finish
            if (!physicsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                physicsExecutor.shutdownNow();
            }
            if (!updateExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                updateExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            physicsExecutor.shutdownNow();
            updateExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}