package com.footballsim.views;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class GameSettingsPanel extends VBox {
    private Spinner<Integer> matchDurationSpinner;
    private Spinner<Double> gameSpeedSpinner;
    private Button resetButton;
    private Label timerLabel;
    private Label redScoreLabel;
    private Label blueScoreLabel;

    public GameSettingsPanel() {
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");

        // Match duration settings
        Label durationLabel = new Label("Match Duration (minutes):");
        matchDurationSpinner = new Spinner<>(1, 30, 5);
        matchDurationSpinner.setEditable(true);

        // Game speed settings
        Label speedLabel = new Label("Game Speed:");
        gameSpeedSpinner = new Spinner<>(0.1, 2.0, 1.0, 0.1);
        gameSpeedSpinner.setEditable(true);

        // Reset button
        resetButton = new Button("Reset Game");
        resetButton.setOnAction(e -> resetGame());

        // Score display
        HBox scoreBox = new HBox(20);
        redScoreLabel = new Label("Red Team: 0");
        blueScoreLabel = new Label("Blue Team: 0");
        scoreBox.getChildren().addAll(redScoreLabel, blueScoreLabel);

        // Timer display
        timerLabel = new Label("Time: 00:00");

        getChildren().addAll(
            durationLabel, matchDurationSpinner,
            speedLabel, gameSpeedSpinner,
            resetButton,
            new Separator(),
            scoreBox,
            timerLabel
        );
    }

    private void resetGame() {
        // TODO: Implement reset functionality
    }

    public void updateScore(int redScore, int blueScore) {
        redScoreLabel.setText("Red Team: " + redScore);
        blueScoreLabel.setText("Blue Team: " + blueScore);
    }

    public void updateTimer(int minutes, int seconds) {
        timerLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }
}
