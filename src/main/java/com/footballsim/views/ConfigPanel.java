package com.footballsim.views;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import java.util.function.BiConsumer;

public class ConfigPanel extends GridPane {
    private Spinner<Integer> widthSpinner;
    private Spinner<Integer> heightSpinner;
    private Button applyButton;
    private ArenaCanvas arenaCanvas;
    private BiConsumer<Double, Double> dimensionsChangedCallback;

    public ConfigPanel(ArenaCanvas canvas) {
        this.arenaCanvas = canvas;

        setPadding(new Insets(10));
        setHgap(10);
        setVgap(10);
        setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");

        // Width control with validation
        Label widthLabel = new Label("Field Width:");
        widthSpinner = new Spinner<>(300, 800, (int)arenaCanvas.getFieldWidth(), 50);
        widthSpinner.setEditable(true);
        widthSpinner.setPrefWidth(100);

        // Height control with validation
        Label heightLabel = new Label("Field Height:");
        heightSpinner = new Spinner<>(200, 600, (int)arenaCanvas.getFieldHeight(), 50);
        heightSpinner.setEditable(true);
        heightSpinner.setPrefWidth(100);

        // Add a small delay to prevent too frequent updates
        widthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateFieldDimensions();
            }
        });

        heightSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateFieldDimensions();
            }
        });

        add(widthLabel, 0, 0);
        add(widthSpinner, 1, 0);
        add(heightLabel, 0, 1);
        add(heightSpinner, 1, 1);
    }

    private void updateFieldDimensions() {
        if (dimensionsChangedCallback != null) {
            dimensionsChangedCallback.accept(
                    widthSpinner.getValue().doubleValue(),
                    heightSpinner.getValue().doubleValue()
            );
        }
    }

    public void setOnDimensionsChanged(BiConsumer<Double, Double> callback) {
        this.dimensionsChangedCallback = callback;
    }
}