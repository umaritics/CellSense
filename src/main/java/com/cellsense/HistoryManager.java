package com.cellsense;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HistoryManager {

    private static final String FILE_PATH = System.getProperty("user.home") + "/cellsense_history.csv";
    private static Map<String, DailyStat> historyData = new HashMap<>();

    // Data Structure for a single day
    public static class DailyStat {
        public String date;
        public int minLevel = 100;
        public int maxLevel = 0;
        public int startLevel = 0; // Level when app first opened today

        public DailyStat(String date) { this.date = date; }
    }

    // Initialize: Load old data and start tracking today
    public static void init() {
        loadData();

        // Start a background thread to update "Today's" stats every minute
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            updateToday();
            saveData();
        }, 0, 1, TimeUnit.MINUTES);
    }

    private static void updateToday() {
        String today = LocalDate.now().toString();
        int currentLevel = BatteryManager.getBatteryLevel();

        DailyStat stat = historyData.getOrDefault(today, new DailyStat(today));

        // Update logic
        if (stat.startLevel == 0) stat.startLevel = currentLevel;
        if (currentLevel < stat.minLevel) stat.minLevel = currentLevel;
        if (currentLevel > stat.maxLevel) stat.maxLevel = currentLevel;

        historyData.put(today, stat);
    }

    public static List<DailyStat> getLast7Days() {
        return historyData.values().stream()
                .sorted(Comparator.comparing(s -> s.date))
                .skip(Math.max(0, historyData.size() - 7)) // Keep last 7
                .collect(Collectors.toList());
    }

    // --- FILE I/O (Simple CSV) ---
    private static void saveData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (DailyStat stat : historyData.values()) {
                writer.println(stat.date + "," + stat.minLevel + "," + stat.maxLevel + "," + stat.startLevel);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void loadData() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    DailyStat stat = new DailyStat(parts[0]);
                    stat.minLevel = Integer.parseInt(parts[1]);
                    stat.maxLevel = Integer.parseInt(parts[2]);
                    stat.startLevel = Integer.parseInt(parts[3]);
                    historyData.put(stat.date, stat);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}