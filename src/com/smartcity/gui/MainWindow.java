package com.smartcity.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainWindow extends Application {

    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();

        Canvas canvas = new Canvas(1200, 800);

        root.setCenter(canvas);

        Scene scene =
                new Scene(root);

        stage.setTitle(
                "Smart City Traffic Simulation");

        stage.setScene(scene);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}