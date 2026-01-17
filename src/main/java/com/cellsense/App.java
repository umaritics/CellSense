package com.cellsense;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.Styles;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class App extends Application {

    private BatteryRing batteryRing;
    private Slider maxSlider;
    private Slider minSlider;
    private ToggleButton alarmToggle;
    private Label alarmStatusLabel;

    // Labels that need live updates
    private Label maxValLabel;
    private Label minValLabel;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // --- LEFT SIDE: BATTERY DASHBOARD ---
        batteryRing = new BatteryRing();

        alarmStatusLabel = new Label("");
        alarmStatusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold; -fx-font-size: 14px;");

        VBox leftPane = new VBox(30, batteryRing, alarmStatusLabel);
        leftPane.setAlignment(Pos.CENTER);
        leftPane.setPadding(new Insets(40));
        // Subtle gradient background for the left side
        leftPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #0d1117, #161b22);");
        HBox.setHgrow(leftPane, Priority.ALWAYS); // Allow it to grow

        // --- RIGHT SIDE: CONTROLS ---
        Label titleLabel = new Label("CellSense");
        // Ocean Blue Color for Title
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2f81f7;");

        VBox settingsPanel = createSettingsPanel();

        VBox rightPane = new VBox(30, titleLabel, settingsPanel);
        rightPane.setAlignment(Pos.CENTER_LEFT);
        rightPane.setPadding(new Insets(40));
        rightPane.setPrefWidth(380);
        rightPane.setStyle("-fx-background-color: #010409;"); // Darker side panel

        // --- ROOT LAYOUT (Split View) ---
        HBox root = new HBox(leftPane, rightPane);

        // --- LOGIC ---
        // 1. Animation on Startup (Fill from 0 to current)
        animateStartup();

        // 2. Periodic Check
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> checkBattery()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // --- WINDOW SETUP ---
        Scene scene = new Scene(root, 800, 500); // Rectangular Desktop Size
        stage.setTitle("CellSense");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void animateStartup() {
        // Start at 0
        batteryRing.setProgress(0, false);

        // After 500ms, animate to real value
        new Timeline(new KeyFrame(Duration.millis(500), e -> {
            int actualLevel = BatteryManager.getBatteryLevel();
            boolean plugged = BatteryManager.isPluggedIn();

            // We animate by slowly increasing the value
            // (For a simple implementation, we just set it now,
            // but the BatteryRing arc transition could be added for smoother visual)
            batteryRing.setProgress(actualLevel, plugged);
        })).play();
    }

    private VBox createSettingsPanel() {
        // --- CONTROL 1: MAX CHARGE ---
        Label maxTitle = new Label("Upper Limit Alarm");
        maxTitle.getStyleClass().add(Styles.TEXT_BOLD);

        maxValLabel = new Label("80%");
        maxValLabel.setStyle("-fx-text-fill: #2f81f7; -fx-font-weight: bold; -fx-font-size: 16px;");

        HBox maxHeader = new HBox(10, maxTitle, maxValLabel); // Side by side

        maxSlider = new Slider(50, 100, 80);
        maxSlider.setBlockIncrement(5);
        maxSlider.valueProperty().addListener((obs, old, val) ->
                maxValLabel.setText(val.intValue() + "%"));

        // --- CONTROL 2: MIN CHARGE ---
        Label minTitle = new Label("Lower Limit Alarm");
        minTitle.getStyleClass().add(Styles.TEXT_BOLD);

        minValLabel = new Label("20%");
        minValLabel.setStyle("-fx-text-fill: #2f81f7; -fx-font-weight: bold; -fx-font-size: 16px;");

        HBox minHeader = new HBox(10, minTitle, minValLabel);

        minSlider = new Slider(5, 50, 20);
        minSlider.valueProperty().addListener((obs, old, val) ->
                minValLabel.setText(val.intValue() + "%"));

        // --- TOGGLE BUTTON ---
        alarmToggle = new ToggleButton("Alarms Active");
        alarmToggle.setSelected(true);
        alarmToggle.setMaxWidth(Double.MAX_VALUE);
        alarmToggle.setPrefHeight(40);
        alarmToggle.getStyleClass().add(Styles.SUCCESS);
        alarmToggle.setOnAction(e -> {
            if (alarmToggle.isSelected()) {
                alarmToggle.setText("Alarms Active");
                alarmToggle.getStyleClass().add(Styles.SUCCESS);
            } else {
                alarmToggle.setText("Alarms Muted");
                alarmToggle.getStyleClass().removeAll(Styles.SUCCESS);
                SoundManager.stopAlarm();
            }
        });

        // Grouping
        VBox panel = new VBox(25,
                new VBox(10, maxHeader, maxSlider),
                new Separator(),
                new VBox(10, minHeader, minSlider),
                new Separator(),
                alarmToggle
        );
        return panel;
    }

    private void checkBattery() {
        int level = BatteryManager.getBatteryLevel();
        boolean isPlugged = BatteryManager.isPluggedIn();

        batteryRing.setProgress(level, isPlugged);

        if (!alarmToggle.isSelected()) {
            alarmStatusLabel.setText("");
            return;
        }

        int maxLimit = (int) maxSlider.getValue();
        int minLimit = (int) minSlider.getValue();

        if (isPlugged && level >= maxLimit) {
            alarmStatusLabel.setText("⚠ LIMIT REACHED - UNPLUG CHARGER");
            SoundManager.playAlarm();
        }
        else if (!isPlugged && level <= minLimit) {
            alarmStatusLabel.setText("⚠ BATTERY LOW - CONNECT CHARGER");
            SoundManager.playAlarm();
        }
        else {
            alarmStatusLabel.setText("");
            SoundManager.stopAlarm();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}