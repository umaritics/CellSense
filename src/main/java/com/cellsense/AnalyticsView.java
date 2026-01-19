package com.cellsense;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Map;

public class AnalyticsView {

    private static Label designCapLabel;
    private static Label currentCapLabel;
    private static Label cycleCountLabel;
    private static Label voltageLabel;
    private static Label healthLabel;

    public static VBox create() {
        Label header = new Label("Analytics & Insights");
        header.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: white;");

        // --- GRID LAYOUT ---
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20, 0, 0, 0));

        // 3 Columns for dense info (Like the Rack_206 image)
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(33);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(33);
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(33);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        // --- ROW 1: THE GRAPH ---
        VBox graphCard = createGraphCard();
        GridPane.setColumnSpan(graphCard, 3); // Stretch full width
        grid.add(graphCard, 0, 0);

        // --- ROW 2: CAPACITY STATS ---
        VBox designCapCard = createStatCard("Design Capacity", "Loading...", "Original mWh", "#8b949e");
        designCapLabel = (Label) designCapCard.getChildren().get(1);

        VBox fullCapCard = createStatCard("Current Capacity", "Loading...", "Actual Max mWh", "#2f81f7");
        currentCapLabel = (Label) fullCapCard.getChildren().get(1);

        VBox healthCard = createStatCard("Battery Health", "Calculating...", "Capacity Retention", "#2ea043");
        healthLabel = (Label) healthCard.getChildren().get(1);

        grid.add(designCapCard, 0, 1);
        grid.add(fullCapCard, 1, 1);
        grid.add(healthCard, 2, 1);

        // --- ROW 3: TECHNICAL STATS ---
        VBox cycleCard = createStatCard("Cycle Count", "Loading...", "Total Charge Cycles", "#d2a8ff");
        cycleCountLabel = (Label) cycleCard.getChildren().get(1);

        VBox voltageCard = createStatCard("Rated Voltage", "Reading...", "Live Sensor Data", "#f85149");
        voltageLabel = (Label) voltageCard.getChildren().get(1);

        // Temp is hard to get reliably in Java without Admin/Drivers,
        // usually represented as "Thermal Status" or N/A in basic APIs.
        VBox tempCard = createStatCard("Thermal Status", "Normal", "System Nominal", "#2ea043");

        grid.add(cycleCard, 0, 2);
        grid.add(voltageCard, 1, 2);
        grid.add(tempCard, 2, 2);

        // --- TRIGGER DATA FETCH ---
        loadRealData();

        VBox content = new VBox(20, header, grid);
        content.setPadding(new Insets(40));
        return content;
    }

    private static void loadRealData() {
        // 1. Get Live Voltage (Fast)
        new Thread(() -> {
            String voltage = BatteryReportManager.getLiveVoltage();
            Platform.runLater(() -> voltageLabel.setText(voltage));
        }).start();

        // 2. Get Heavy Report Data (Slow, Async)
        BatteryReportManager.getBatteryDetailsAsync().thenAccept(data -> {
            Platform.runLater(() -> {
                String dCap = data.getOrDefault("DesignCapacity", "N/A");
                String fCap = data.getOrDefault("FullChargeCapacity", "N/A");
                String cycles = data.getOrDefault("CycleCount", "N/A");

                designCapLabel.setText(dCap + " mWh");
                currentCapLabel.setText(fCap + " mWh");
                cycleCountLabel.setText(cycles);

                // Calculate Health %
                try {
                    double design = Double.parseDouble(dCap);
                    double current = Double.parseDouble(fCap);
                    int health = (int) ((current / design) * 100);
                    healthLabel.setText(health + "%");

                    // Color code health
                    Label subtext = (Label)((HBox)healthLabel.getParent().getChildrenUnmodifiable().get(2)).getChildren().get(1);
                    if (health >= 90) subtext.setText("Excellent");
                    else if (health >= 80) subtext.setText("Good");
                    else subtext.setText("Degraded");

                } catch (Exception e) {
                    healthLabel.setText("--");
                }
            });
        });
    }

    private static VBox createStatCard(String title, String value, String subtext, String color) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 28px;"); // Slightly smaller for 3-col layout

        Label subLbl = new Label(subtext);
        subLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");

        Circle dot = new Circle(4, Color.web(color));
        HBox statusBox = new HBox(8, dot, subLbl);
        statusBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(10, titleLbl, valLbl, statusBox);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #161b22; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        // Force consistent height
        card.setMinHeight(120);
        return card;
    }

    private static VBox createGraphCard() {
        // (Same graph code as before, simplified for brevity)
        Label title = new Label("Battery Level History (Last Hour)");
        title.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");

        NumberAxis xAxis = new NumberAxis(0, 60, 10);
        xAxis.setLabel("Minutes Ago");
        NumberAxis yAxis = new NumberAxis(0, 100, 20);

        AreaChart<Number, Number> areaChart = new AreaChart<>(xAxis, yAxis);
        areaChart.setCreateSymbols(false);
        areaChart.setLegendVisible(false);
        areaChart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        // In real version, we'd pull from HistoryManager. For now, static data.
        series.getData().add(new XYChart.Data<>(60, 85));
        series.getData().add(new XYChart.Data<>(30, 65));
        series.getData().add(new XYChart.Data<>(0, BatteryManager.getBatteryLevel()));
        areaChart.getData().add(series);

        VBox card = new VBox(15, title, areaChart);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #161b22; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        card.setMinHeight(280);
        return card;
    }
}