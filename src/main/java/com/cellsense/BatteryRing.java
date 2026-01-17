package com.cellsense;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class BatteryRing extends StackPane {
    private final Arc progressArc;
    private final Label percentageLabel;
    private final Label statusLabel;

    // Remember current value to animate from it
    private double currentDisplayValue = 0;

    public BatteryRing() {
        // Background Circle
        Circle track = new Circle(100); // Slightly larger
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#21262d")); // Dark grey
        track.setStrokeWidth(18);

        // Progress Arc
        progressArc = new Arc();
        progressArc.setRadiusX(100);
        progressArc.setRadiusY(100);
        progressArc.setStartAngle(90);
        progressArc.setLength(0);
        progressArc.setType(javafx.scene.shape.ArcType.OPEN);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStroke(Color.web("#2f81f7"));
        progressArc.setStrokeWidth(18);
        progressArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        percentageLabel = new Label("0%");
        percentageLabel.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: white;");

        statusLabel = new Label("Scanning");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #8b949e;");
        statusLabel.setTranslateY(50);

        this.getChildren().addAll(track, progressArc, percentageLabel, statusLabel);
        this.setAlignment(Pos.CENTER);
    }

    public void setProgress(int targetPercentage, boolean isPlugged) {
        // 1. Text Update
        percentageLabel.setText(targetPercentage + "%");

        // 2. Color Logic
        if (isPlugged) {
            progressArc.setStroke(Color.web("#2ea043")); // Green
            statusLabel.setText("⚡ Charging");
            statusLabel.setTextFill(Color.web("#2ea043"));
        } else {
            statusLabel.setText("On Battery");
            statusLabel.setTextFill(Color.web("#8b949e"));
            if (targetPercentage <= 20) {
                progressArc.setStroke(Color.web("#f85149")); // Red
            } else {
                progressArc.setStroke(Color.web("#2f81f7")); // Blue
            }
        }

        // 3. Smooth Animation
        double targetLength = -(360 * (targetPercentage / 100.0));

        // Only animate if value changed significantly
        if (Math.abs(targetLength - currentDisplayValue) > 1) {
            Timeline animate = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(progressArc.lengthProperty(), currentDisplayValue)),
                    new KeyFrame(Duration.millis(800), new KeyValue(progressArc.lengthProperty(), targetLength)) // 800ms smooth transition
            );
            animate.play();
            currentDisplayValue = targetLength;
        }
    }
}