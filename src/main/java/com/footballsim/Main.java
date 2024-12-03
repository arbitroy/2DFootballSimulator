package com.footballsim;

import javafx.application.Application;
import javafx.stage.Stage;
import com.footballsim.views.MainWindow;

public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            MainWindow mainWindow = new MainWindow(primaryStage);
            mainWindow.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}