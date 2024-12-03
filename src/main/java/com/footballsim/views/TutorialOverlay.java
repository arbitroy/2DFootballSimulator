package com.footballsim.views;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class TutorialOverlay extends Stage {
    private VBox content;

    public TutorialOverlay(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Game Tutorial");

        content = new VBox(10);
        content.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);

        addTutorialContent();

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(scrollPane, closeButton);

        setScene(new Scene(root, 500, 400));
    }

    private void addTutorialContent() {
        addSection("Game Controls", 
            "- Use Start/Pause to control the game\n" +
            "- Click on the field to kick the ball\n" +
            "- Add robots using the control panel\n" +
            "- Place obstacles to create challenges"
        );

        addSection("Robots", 
            "- Red and Blue teams available\n" +
            "- Adjust robot speed and sensor range\n" +
            "- Robots automatically seek the ball\n" +
            "- Sensors help avoid obstacles"
        );

        addSection("Scoring",
            "- Score by getting the ball in the opponent's goal\n" +
            "- Match duration can be set before starting\n" +
            "- Game speed can be adjusted\n" +
            "- Use Reset to start over"
        );
    }

    private void addSection(String title, String content) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        TextArea contentArea = new TextArea(content);
        contentArea.setWrapText(true);
        contentArea.setEditable(false);
        contentArea.setPrefRowCount(4);
        
        this.content.getChildren().addAll(titleLabel, contentArea, new Separator());
    }
}
