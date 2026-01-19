package com.cellsense;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class BatteryLiquid extends StackPane {

    private static final double WIDTH = 200;
    private static final double HEIGHT = 300;

    private final SVGPath waveBack;
    private final SVGPath waveFront;
    private final Label percentageLabel;
    private final Label statusLabel;
    private final Pane liquidPane; // Holds the waves

    // The total height of the liquid area inside the battery
    private final double LIQUID_MAX_HEIGHT = HEIGHT - 20;

    public BatteryLiquid() {
        // 1. The Battery Container (Shell)
        Rectangle shell = new Rectangle(WIDTH, HEIGHT);
        shell.setArcWidth(40);
        shell.setArcHeight(40);
        shell.setFill(Color.TRANSPARENT);
        shell.setStroke(Color.web("#30363d"));
        shell.setStrokeWidth(6);

        // 2. The Battery "Cap" (Nub on top)
        Rectangle cap = new Rectangle(60, 15);
        cap.setArcWidth(5);
        cap.setArcHeight(5);
        cap.setFill(Color.web("#30363d"));
        cap.setTranslateY(-(HEIGHT / 2) - 10); // Move to top

        // 3. The Liquid Waves
        // We use SVG paths to draw a sine wave
        // M=Move, Q=Quadratic Curve. This draws a long wave pattern.
        String wavePath = "M 0 0 Q 50 20 100 0 T 200 0 T 300 0 T 400 0 V 400 H 0 Z";

        waveBack = new SVGPath();
        waveBack.setContent(wavePath);
        waveBack.setFill(Color.web("#1f6feb", 0.3)); // Translucent Blue

        waveFront = new SVGPath();
        waveFront.setContent(wavePath);
        waveFront.setFill(Color.web("#2f81f7")); // Solid Blue (matches app theme)

        // Wrapper for liquid to handle the "Level" (Y position)
        liquidPane = new Pane(waveBack, waveFront);
        liquidPane.setMaxSize(WIDTH - 12, HEIGHT - 12);

        // CLIP: Force the liquid to stay inside the rounded battery shape
        Rectangle clip = new Rectangle(WIDTH - 12, HEIGHT - 12);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        liquidPane.setClip(clip);

        // 4. Text Labels
        percentageLabel = new Label("0%");
        percentageLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 52));
        percentageLabel.setTextFill(Color.WHITE);
        percentageLabel.setEffect(new DropShadow(10, Color.BLACK)); // Shadow for readability

        statusLabel = new Label("Scanning...");
        statusLabel.setFont(Font.font("Segoe UI", 16));
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setTranslateY(40);
        statusLabel.setEffect(new DropShadow(5, Color.BLACK));

        // 5. Animations
        // Animate waves horizontally to look like flowing liquid
        setupWaveAnimation(waveBack, 3000, 50); // Slower, offset
        setupWaveAnimation(waveFront, 2000, 0); // Faster

        // Combine everything
        this.getChildren().addAll(cap, shell, liquidPane, percentageLabel, statusLabel);
        this.setAlignment(Pos.CENTER);

        // Default level: Empty
        setLiquidLevel(0);
    }

    private void setupWaveAnimation(SVGPath wave, double durationMs, double delay) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(durationMs), wave);
        transition.setFromX(-100); // Start slightly left
        transition.setToX(0);      // Move right
        transition.setCycleCount(Animation.INDEFINITE);
        transition.setAutoReverse(true); // Move back and forth for a "sloshing" effect
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.play();
    }

    public void setProgress(int percentage, boolean isPlugged) {
        percentageLabel.setText(percentage + "%");

        // Calculate color based on state
        String colorHex = "#2f81f7"; // Default Blue
        if (isPlugged) {
            colorHex = "#2ea043"; // Green
            statusLabel.setText("⚡ Charging");
        } else if (percentage <= 20) {
            colorHex = "#f85149"; // Red
            statusLabel.setText("On Battery");
        } else {
            colorHex = "#2f81f7"; // Blue
            statusLabel.setText("On Battery");
        }

        // Update Wave Colors
        waveFront.setFill(Color.web(colorHex));
        waveBack.setFill(Color.web(colorHex, 0.3)); // 30% opacity

        // Update Level (Animation)
        setLiquidLevel(percentage);
    }

    private void setLiquidLevel(int percentage) {
        // Calculate how far down the waves should be
        // 100% = Top (Y=0), 0% = Bottom (Y=MAX)
        // We add an offset (-20) so the wave crests don't get clipped at 100%
        double level = LIQUID_MAX_HEIGHT - ((percentage / 100.0) * LIQUID_MAX_HEIGHT);

        // We simply move the waves UP/DOWN inside the pane
        // The path itself is drawn at Y=0, so we translate it down to the "empty" space
        TranslateTransition tBack = new TranslateTransition(Duration.millis(800), waveBack);
        tBack.setToY(level - 10); // Offset slightly
        tBack.play();

        TranslateTransition tFront = new TranslateTransition(Duration.millis(800), waveFront);
        tFront.setToY(level);
        tFront.play();
    }
}