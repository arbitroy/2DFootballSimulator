package com.footballsim.views;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.File;
import java.io.IOException;
import com.footballsim.controllers.GameCoordinator;
import com.footballsim.models.GameConfig;
import com.footballsim.models.TeamRobot;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Main window of the football simulator application.
 * Manages the overall UI layout and component interactions.
 */
public class MainWindow {
    private Stage stage;
    private VBox root;
    private MenuBar menuBar;
    private ToolBar toolBar;
    private BorderPane mainContent;

    private ArenaCanvas arenaCanvas;
    private GameSettingsPanel settingsPanel;
    private RobotControlPanel robotPanel;
    private ObstacleControlPanel obstaclePanel;
    private ConfigPanel configPanel;

    private Button startButton;
    private Button pauseButton;

    private GameCoordinator gameCoordinator;

    private MenuItem saveMenuItem;
    private MenuItem loadMenuItem;
    private MenuItem exitMenuItem;
    private MenuItem helpMenuItem;
    private MenuItem aboutMenuItem;

    /**
     * Creates a new main window
     * 
     * @param stage The primary stage from JavaFX
     */
    public MainWindow(Stage stage) {
        this.stage = stage;
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    /**
     * Initializes all UI components and controllers
     */
    private void initializeComponents() {
        // Initialize all UI components first
        root = new VBox();
        menuBar = createMenuBar();

        // Create main game components
        arenaCanvas = new ArenaCanvas();
        settingsPanel = new GameSettingsPanel();
        robotPanel = new RobotControlPanel();
        obstaclePanel = new ObstacleControlPanel();
        configPanel = new ConfigPanel(arenaCanvas);

        // Create toolbar
        toolBar = createToolBar();

        // Initialize game coordinator last, after all components are created
        gameCoordinator = new GameCoordinator(arenaCanvas, settingsPanel, robotPanel);

        // Connect toolbar buttons to coordinator
        startButton.setOnAction(e -> {
            gameCoordinator.startGame();
            startButton.setDisable(true);
            pauseButton.setDisable(false);
        });

        pauseButton.setOnAction(e -> {
            gameCoordinator.stopGame();
            startButton.setDisable(false);
            pauseButton.setDisable(true);
        });

        // Connect config panel to coordinator
        configPanel.setOnDimensionsChanged((width, height) -> gameCoordinator.updateFieldDimensions(width, height));
    }

    /**
     * Creates the menu bar with all menus and items
     * 
     * @return Configured MenuBar
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu("File");
        saveMenuItem = new MenuItem("Save");
        loadMenuItem = new MenuItem("Load");
        exitMenuItem = new MenuItem("Exit");
        fileMenu.getItems().addAll(saveMenuItem, loadMenuItem,
                new SeparatorMenuItem(), exitMenuItem);

        // View Menu
        Menu viewMenu = new Menu("View");
        MenuItem debugMenuItem = new MenuItem("Toggle Debug Info");
        debugMenuItem.setOnAction(e -> gameCoordinator.toggleDebugInfo());
        viewMenu.getItems().add(debugMenuItem);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        helpMenuItem = new MenuItem("Help");
        aboutMenuItem = new MenuItem("About");
        helpMenu.getItems().addAll(helpMenuItem, aboutMenuItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu, helpMenu);
        return menuBar;
    }

    /**
     * Creates the toolbar with all buttons and controls
     * 
     * @return Configured ToolBar
     */
    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        startButton = new Button("Start");
        pauseButton = new Button("Pause");
        pauseButton.setDisable(true);

        Button addRedRobotButton = new Button("Add Red Robot");
        Button addBlueRobotButton = new Button("Add Blue Robot");
        Button addObstacleButton = new Button("Add Obstacle");
        Button removeButton = new Button("Remove Selected");

      addRedRobotButton.setOnAction(e -> {
    TeamRobot robot = robotManager.addRobot(true, TeamRobot.RobotRole.ATTACKER);
    robotPanel.addRobot(true); // Updates the UI list
});

addBlueRobotButton.setOnAction(e -> {
    TeamRobot robot = robotManager.addRobot(false, TeamRobot.RobotRole.ATTACKER);
    robotPanel.addRobot(false); // Updates the UI list
});
        addObstacleButton.setOnAction(e -> obstaclePanel.addObstacle());
        removeButton.setOnAction(e -> gameCoordinator.removeSelectedObject());

        Button helpButton = new Button("Show Tutorial");
        helpButton.setOnAction(e -> new TutorialOverlay(stage).show());

        toolBar.getItems().addAll(
                startButton,
                pauseButton,
                new Separator(),
                addRedRobotButton,
                addBlueRobotButton,
                addObstacleButton,
                removeButton,
                new Separator(),
                helpButton);

        return toolBar;
    }

    /**
     * Lays out all components in the window
     */
    private void layoutComponents() {
        root.getChildren().addAll(menuBar, toolBar);

        // Create side panels
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.getChildren().addAll(configPanel, settingsPanel);

        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.getChildren().addAll(robotPanel, obstaclePanel);

        // Main content layout
        mainContent = new BorderPane();
        mainContent.setLeft(leftPanel);
        mainContent.setCenter(arenaCanvas);
        mainContent.setRight(rightPanel);

        root.getChildren().add(mainContent);

        // Create scene
        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.setTitle("2D Football Simulator");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
    }

    /**
     * Sets up event handlers for menu items and other controls
     */
    private void setupEventHandlers() {
        // File menu handlers
        exitMenuItem.setOnAction(e -> stage.close());
        saveMenuItem.setOnAction(e -> handleSave());
        loadMenuItem.setOnAction(e -> handleLoad());

        // Help menu handlers
        helpMenuItem.setOnAction(e -> showHelp());
        aboutMenuItem.setOnAction(e -> showAbout());

        // Window closing handler
        stage.setOnCloseRequest(e -> {
            if (gameCoordinator.isGameRunning()) {
                e.consume(); // Prevent immediate closing
                confirmExit();
            }
        });
    }

    /**
     * Handles saving game configuration
     */
    private void handleSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Game Configuration");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Game Files", "*.game"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                GameConfig config = new GameConfig();
                gameCoordinator.saveGameState(config);
                config.saveToFile(file);

                showSuccess("Game configuration saved successfully!");
            } catch (IOException e) {
                showError("Error saving game", e.getMessage());
            }
        }
    }

    /**
     * Handles loading game configuration
     */
    private void handleLoad() {
        if (gameCoordinator.isGameRunning()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Load");
            alert.setHeaderText("Loading will stop the current game");
            alert.setContentText("Do you want to continue?");

            if (alert.showAndWait().get() != ButtonType.OK) {
                return;
            }
            gameCoordinator.stopGame();
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Game Configuration");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Game Files", "*.game"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                GameConfig config = GameConfig.loadFromFile(file);
                gameCoordinator.loadGameState(config);
                showSuccess("Game configuration loaded successfully!");
            } catch (IOException | ClassNotFoundException e) {
                showError("Error loading game", e.getMessage());
            }
        }
    }

    /**
     * Shows help dialog
     */
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("How to Use the Simulator");
        alert.setContentText(
                "1. Use the toolbar buttons to add robots and obstacles\n" +
                        "2. Click Start to begin the simulation\n" +
                        "3. Use Pause to pause the game\n" +
                        "4. Click on the field to kick the ball\n" +
                        "5. Drag robots to reposition them\n" +
                        "6. Save your configuration using File -> Save");
        alert.showAndWait();
    }

    /**
     * Shows about dialog
     */
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("2D Football Simulator");
        alert.setContentText("Version 1.0\nCreated for Advanced Programming");
        alert.showAndWait();
    }

    /**
     * Shows confirmation dialog when exiting during game
     */
    private void confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Exit");
        alert.setHeaderText("A game is currently running");
        alert.setContentText("Do you want to exit anyway?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            stage.close();
        }
    }

    /**
     * Shows error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows success dialog
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows the window
     */
    public void show() {
        stage.show();
    }


}