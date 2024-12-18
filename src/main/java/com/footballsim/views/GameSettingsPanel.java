package com.footballsim.views;

import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class GameSettingsPanel extends VBox {
    private final Spinner<Integer> matchDurationSpinner;
    private final Spinner<Double> gameSpeedSpinner;
    private final Button resetButton;
    private final Label timerLabel;
    private final Label redScoreLabel;
    private final Label blueScoreLabel;
    private Consumer<Integer> onDurationChanged;
    private Consumer<Double> onSpeedChanged;


    private Runnable onReset;

    public GameSettingsPanel() {
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");

        // Match duration settings
        Label durationLabel = new Label("Match Duration (minutes):");
        matchDurationSpinner = new Spinner<>(1, 30, 5); // min, max, default
        matchDurationSpinner.setEditable(true);
        matchDurationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int minutes = newVal;
                if (minutes >= 1 && minutes <= 30) {
                    // Update display
                    updateTimer(minutes, 0);
                    // Notify of change (minutes to seconds)
                    if (onDurationChanged != null) {
                        onDurationChanged.accept(minutes * 60);
                    }
                } else {
                    Platform.runLater(() -> matchDurationSpinner.getValueFactory().setValue(oldVal));
                }
            }
        });

        // Game speed settings
        Label speedLabel = new Label("Game Speed:");
        gameSpeedSpinner = new Spinner<>(0.1, 2.0, 1.0, 0.1); // min, max, default, step
        gameSpeedSpinner.setEditable(true);
        gameSpeedSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal >= 0.1 && newVal <= 2.0) {
                    if (onSpeedChanged != null) {
                        onSpeedChanged.accept(newVal);
                    }
                } else {
                    Platform.runLater(() -> gameSpeedSpinner.getValueFactory().setValue(oldVal));
                }
            }
        });

        // Reset button
        resetButton = new Button("Reset Game");

        // Score display
        HBox scoreBox = new HBox(20);
        redScoreLabel = new Label("Red Team: 0");
        blueScoreLabel = new Label("Blue Team: 0");
        scoreBox.getChildren().addAll(redScoreLabel, blueScoreLabel);

        // Timer display
        int initialMinutes = matchDurationSpinner.getValue();
        timerLabel = new Label(String.format("Time: %02d:00", initialMinutes));

        getChildren().addAll(
                durationLabel, matchDurationSpinner,
                speedLabel, gameSpeedSpinner,
                resetButton,
                new Separator(),
                scoreBox,
                timerLabel);
    }

    public void setOnReset(Runnable handler) {
        this.onReset = handler;
        resetButton.setOnAction(e -> {
            if (onReset != null) {
                onReset.run();
            }
        });
    }

    public void updateScore(int redScore, int blueScore) {
        Platform.runLater(() -> {
            redScoreLabel.setText("Red Team: " + redScore);
            blueScoreLabel.setText("Blue Team: " + blueScore);
        });
    }

    public void updateTimer(int minutes, int seconds) {
        Platform.runLater(() -> {
            timerLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
        });
    }

    public void setMatchDuration(int seconds) {
        int minutes = seconds / 60;
        Platform.runLater(() -> {
            matchDurationSpinner.getValueFactory().setValue(minutes);
        });
    }

    public void setGameSpeed(double speed) {
        Platform.runLater(() -> {
            gameSpeedSpinner.getValueFactory().setValue(speed);
        });
    }

    public void setOnSpeedChanged(Consumer<Double> handler) {
        this.onSpeedChanged = handler;
    }

    public void setOnDurationChanged(Consumer<Integer> handler) {
        this.onDurationChanged = handler;
    }

    public int getMatchDuration() {
        return matchDurationSpinner.getValue() * 60; // Convert minutes to seconds
    }

    public double getGameSpeed() {
        return gameSpeedSpinner.getValue();
    }
}