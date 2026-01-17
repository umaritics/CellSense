package com.cellsense;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

public class BatteryRing extends StackPane {
    private final Arc progressArc;
    private final Label percentageLabel;
    private final Label statusLabel;
    private double currentDisplayValue = 0;

    public BatteryRing() {
        // 1. Background Track (Changed from Circle to Arc for perfect alignment)
        Arc track = new Arc();
        track.setRadiusX(90);
        track.setRadiusY(90);
        track.setStartAngle(90);
        track.setLength(360); // Full circle
        track.setType(javafx.scene.shape.ArcType.OPEN);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#21262d")); // Dark Grey
        track.setStrokeWidth(18);
        track.setStrokeType(StrokeType.CENTERED);
        track.setStrokeLineCap(StrokeLineCap.BUTT); // Flat ends for track

        // 2. Progress Arc
        progressArc = new Arc();
        progressArc.setRadiusX(90);
        progressArc.setRadiusY(90);
        progressArc.setStartAngle(90);
        progressArc.setLength(0);
        progressArc.setType(javafx.scene.shape.ArcType.OPEN);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStroke(Color.web("#2f81f7"));
        progressArc.setStrokeWidth(18);
        progressArc.setStrokeType(StrokeType.CENTERED);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND); // Rounded ends

        // Add a glow effect
        progressArc.setEffect(new DropShadow(15, Color.web("#2f81f7", 0.4)));

        // 3. Labels
        percentageLabel = new Label("0%");
        percentageLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: white;");

        statusLabel = new Label("Scanning");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8b949e;");
        statusLabel.setTranslateY(45);

        // Ensure everything is centered
        this.getChildren().addAll(track, progressArc, percentageLabel, statusLabel);
        this.setAlignment(Pos.CENTER);
    }

    public void setProgress(int targetPercentage, boolean isPlugged) {
        percentageLabel.setText(targetPercentage + "%");

        if (isPlugged) {
            progressArc.setStroke(Color.web("#2ea043")); // Green
            progressArc.setEffect(new DropShadow(20, Color.web("#2ea043", 0.5)));
            statusLabel.setText("⚡ Charging");
            statusLabel.setStyle("-fx-text-fill: #2ea043; -fx-font-size: 14px;");
        } else {
            statusLabel.setText("On Battery");
            statusLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 14px;");
            if (targetPercentage <= 20) {
                progressArc.setStroke(Color.web("#f85149")); // Red
                progressArc.setEffect(new DropShadow(20, Color.web("#f85149", 0.5)));
            } else {
                progressArc.setStroke(Color.web("#2f81f7")); // Blue
                progressArc.setEffect(new DropShadow(20, Color.web("#2f81f7", 0.5)));
            }
        }

        double targetLength = -(360 * (targetPercentage / 100.0));
        if (Math.abs(targetLength - currentDisplayValue) > 1) {
            Timeline animate = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(progressArc.lengthProperty(), currentDisplayValue)),
                    new KeyFrame(Duration.millis(800), new KeyValue(progressArc.lengthProperty(), targetLength))
            );
            animate.play();
            currentDisplayValue = targetLength;
        }
    }
}