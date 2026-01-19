package com.cellsense;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoryView {

    public static VBox create() {
        Label header = new Label("System History Report");
        header.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: white;");

        // --- CHART ---
        Label chartTitle = new Label("Battery Drain History");
        chartTitle.getStyleClass().add(Styles.TITLE_4);
        chartTitle.setStyle("-fx-text-fill: #8b949e;");

        // X-Axis: Time
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time");
        xAxis.setForceZeroInRange(false);

        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                try {
                    LocalDateTime date = LocalDateTime.ofEpochSecond(object.longValue(), 0, ZoneOffset.UTC);
                    return date.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
                } catch (Exception e) { return ""; }
            }
            @Override
            public Number fromString(String string) { return 0; }
        });

        NumberAxis yAxis = new NumberAxis(0, 100, 25);
        yAxis.setLabel("Capacity %");

        AreaChart<Number, Number> areaChart = new AreaChart<>(xAxis, yAxis);
        areaChart.setLegendVisible(false);
        areaChart.setCreateSymbols(false);
        areaChart.setPrefHeight(300);
        areaChart.setStyle("-fx-background-color: transparent;");

        // --- TABLES ---
        Label usageTitle = new Label("Recent Usage Details");
        usageTitle.getStyleClass().add(Styles.TITLE_4);
        usageTitle.setStyle("-fx-text-fill: #8b949e; -fx-padding: 20 0 0 0;");

        TableView<BatteryReportManager.UsageRecord> usageTable = new TableView<>();
        usageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        usageTable.getStyleClass().addAll(Styles.STRIPED, Styles.DENSE);
        usageTable.setPrefHeight(300);

        TableColumn<BatteryReportManager.UsageRecord, String> timeCol = new TableColumn<>("Start Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        TableColumn<BatteryReportManager.UsageRecord, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
        TableColumn<BatteryReportManager.UsageRecord, String> sourceCol = new TableColumn<>("Source");
        sourceCol.setCellValueFactory(new PropertyValueFactory<>("source"));
        TableColumn<BatteryReportManager.UsageRecord, String> capCol = new TableColumn<>("Capacity %");
        capCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        TableColumn<BatteryReportManager.UsageRecord, String> mwhCol = new TableColumn<>("Remaining mWh");
        mwhCol.setCellValueFactory(new PropertyValueFactory<>("mWh"));

        usageTable.getColumns().addAll(timeCol, stateCol, sourceCol, capCol, mwhCol);

        Label capTitle = new Label("Battery Capacity History");
        capTitle.getStyleClass().add(Styles.TITLE_4);
        capTitle.setStyle("-fx-text-fill: #8b949e; -fx-padding: 20 0 0 0;");

        TableView<BatteryReportManager.CapacityRecord> capTable = new TableView<>();
        capTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        capTable.getStyleClass().addAll(Styles.STRIPED, Styles.DENSE);
        capTable.setPrefHeight(300);

        TableColumn<BatteryReportManager.CapacityRecord, String> dateCol = new TableColumn<>("Period");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<BatteryReportManager.CapacityRecord, String> fullCol = new TableColumn<>("Full Charge Cap.");
        fullCol.setCellValueFactory(new PropertyValueFactory<>("fullCharge"));
        TableColumn<BatteryReportManager.CapacityRecord, String> designCol = new TableColumn<>("Design Cap.");
        designCol.setCellValueFactory(new PropertyValueFactory<>("designCapacity"));

        capTable.getColumns().addAll(dateCol, fullCol, designCol);

        // --- LOAD DATA ---
        BatteryReportManager.getFullReportAsync().thenAccept(data -> {
            Platform.runLater(() -> {
                usageTable.setItems(data.recentUsage);
                capTable.setItems(data.capacityHistory);

                // USE THE NEW HIGH-RES DATA
                populateChart(areaChart, data.drainGraph);
            });
        });

        VBox content = new VBox(15, chartTitle, areaChart, usageTitle, usageTable, capTitle, capTable);
        content.setPadding(new Insets(0, 20, 40, 0));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox root = new VBox(20, header, scroll);
        root.setPadding(new Insets(40));
        return root;
    }

    private static void populateChart(AreaChart<Number, Number> chart, List<BatteryReportManager.GraphPoint> points) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        // The script data format is always ISO: 2025-11-03T19:18:24
        DateTimeFormatter parser = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (BatteryReportManager.GraphPoint p : points) {
            try {
                LocalDateTime date = LocalDateTime.parse(p.timestamp, parser);
                long epoch = date.toEpochSecond(ZoneOffset.UTC);
                series.getData().add(new XYChart.Data<>(epoch, p.percentage));
            } catch (Exception e) { }
        }
        chart.getData().add(series);
    }
}