package com.cellsense;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BatteryReportManager {

    // --- DATA MODELS ---

    //Represents a single point on the graph
    public static class GraphPoint {
        public String timestamp; // ISO format: 2025-11-03T19:18:24
        public double percentage; // 0.0 to 100.0

        public GraphPoint(String timestamp, double percentage) {
            this.timestamp = timestamp;
            this.percentage = percentage;
        }
    }

    public static class UsageRecord {
        public String startTime;
        public String state;
        public String source;
        public String capacity;
        public String mWh;

        public UsageRecord(String start, String state, String src, String cap, String mwh) {
            this.startTime = cleanHtml(start);
            this.state = cleanHtml(state);
            this.source = cleanHtml(src);
            this.capacity = cleanHtml(cap);
            this.mWh = cleanHtml(mwh);
        }
        public String getStartTime() { return startTime; }
        public String getState() { return state; }
        public String getSource() { return source; }
        public String getCapacity() { return capacity; }
        public String getMWh() { return mWh; }
    }

    public static class CapacityRecord {
        public String date;
        public String fullCharge;
        public String designCapacity;

        public CapacityRecord(String date, String full, String design) {
            this.date = cleanHtml(date);
            this.fullCharge = cleanHtml(full);
            this.designCapacity = cleanHtml(design);
        }
        public String getDate() { return date; }
        public String getFullCharge() { return fullCharge; }
        public String getDesignCapacity() { return designCapacity; }
    }

    public static class ReportData {
        public ObservableList<GraphPoint> drainGraph = FXCollections.observableArrayList(); // NEW
        public ObservableList<UsageRecord> recentUsage = FXCollections.observableArrayList();
        public ObservableList<CapacityRecord> capacityHistory = FXCollections.observableArrayList();
    }

    // --- MAIN METHODS ---

    public static CompletableFuture<Map<String, String>> getBatteryDetailsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> data = new HashMap<>();
            File reportFile = null;
            try {
                reportFile = generateHtmlReport();
                String htmlContent = readFileSafe(reportFile);

                String dCap = extractSimpleValue(htmlContent, "DESIGN CAPACITY");
                String fCap = extractSimpleValue(htmlContent, "FULL CHARGE CAPACITY");
                String cycles = extractSimpleValue(htmlContent, "CYCLE COUNT");

                data.put("DesignCapacity", cleanNumberString(dCap));
                data.put("FullChargeCapacity", cleanNumberString(fCap));
                data.put("CycleCount", cleanNumberString(cycles));

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(reportFile != null && reportFile.exists()) reportFile.delete();
            }
            return data;
        });
    }

    public static CompletableFuture<ReportData> getFullReportAsync() {
        return CompletableFuture.supplyAsync(() -> {
            ReportData data = new ReportData();
            File reportFile = null;
            try {
                reportFile = generateHtmlReport();
                String htmlContent = readFileSafe(reportFile);

                // 1. Parse Graph Data
                parseDrainGraph(htmlContent, data.drainGraph);

                // 2. Parse Tables
                String recentUsageSection = getSection(htmlContent, "Recent usage", "Battery usage");
                parseRecentUsage(recentUsageSection, data.recentUsage);

                String capacitySection = getSection(htmlContent, "Battery capacity history", "Battery life estimates");
                parseCapacityHistory(capacitySection, data.capacityHistory);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(reportFile != null && reportFile.exists()) reportFile.delete();
            }
            return data;
        });
    }

    // --- PARSING LOGIC ---

    //Extracts the 'drainGraphData' JSON array from the script tag
    private static void parseDrainGraph(String html, ObservableList<GraphPoint> list) {
        if (html == null) return;

        // Find the variable definition
        // drainGraphData = [ ... ];
        int startVar = html.indexOf("drainGraphData");
        if (startVar == -1) return;

        int startArr = html.indexOf("[", startVar);
        int endArr = html.indexOf("];", startArr);

        if (startArr != -1 && endArr != -1) {
            String jsonBlock = html.substring(startArr, endArr + 1);

            // Regex to match objects like: { x0: "2025...", ... y0: 0.65... }
            Pattern p = Pattern.compile("x0:\\s*\"(.*?)\".*?y0:\\s*([0-9\\.]+)", Pattern.DOTALL);
            Matcher m = p.matcher(jsonBlock);

            while (m.find()) {
                try {
                    String time = m.group(1);
                    double percent = Double.parseDouble(m.group(2)) * 100.0; // Convert 0.65 -> 65.0
                    list.add(new GraphPoint(time, percent));
                } catch (Exception ignored) {}
            }
        }
    }

    private static void parseRecentUsage(String html, ObservableList<UsageRecord> list) {
        if (html == null) return;
        Pattern rowPattern = Pattern.compile(
                "<span class=\"date\">([\\s\\S]*?)</span>\\s*<span class=\"time\">([\\s\\S]*?)</span>[\\s\\S]*?class=\"state\">\\s*([\\s\\S]*?)\\s*</td>[\\s\\S]*?class=\"acdc\">\\s*([\\s\\S]*?)\\s*</td>[\\s\\S]*?class=\"percent\">\\s*([\\s\\S]*?)\\s*</td>[\\s\\S]*?class=\"mw\">\\s*([\\s\\S]*?)\\s*</td>",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = rowPattern.matcher(html);
        String lastDate = "";

        while (m.find()) {
            String date = m.group(1).trim();
            String time = m.group(2).trim();
            if (date.isEmpty()) date = lastDate; else lastDate = date;

            list.add(0, new UsageRecord(date + " " + time, m.group(3), m.group(4), m.group(5), m.group(6)));
        }
    }

    private static void parseCapacityHistory(String html, ObservableList<CapacityRecord> list) {
        if (html == null) return;
        Pattern rowPattern = Pattern.compile(
                "<td[^>]*class=\"dateTime\"[^>]*>([\\s\\S]*?)</td>\\s*<td[^>]*class=\"mw\"[^>]*>([\\s\\S]*?)</td>\\s*<td[^>]*class=\"mw\"[^>]*>([\\s\\S]*?)</td>",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = rowPattern.matcher(html);
        while (m.find()) {
            list.add(0, new CapacityRecord(m.group(1), m.group(2), m.group(3)));
        }
    }

    // --- HELPERS ---

    private static String cleanHtml(String raw) {
        if (raw == null) return "-";
        return raw.replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    private static String extractSimpleValue(String html, String label) {
        try {
            Pattern p = Pattern.compile(label + "[\\s\\S]*?</td>\\s*<td>\\s*([\\s\\S]*?)\\s*</td>", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(html);
            if (m.find()) return m.group(1).trim();
        } catch (Exception e) {}
        return "0";
    }

    private static String getSection(String html, String startHeader, String nextHeader) {
        try {
            int start = html.indexOf(startHeader);
            if (start == -1) return null;
            int end = html.indexOf(nextHeader, start);
            if (end == -1) end = html.length();
            return html.substring(start, end);
        } catch (Exception e) {}
        return null;
    }

    private static String cleanNumberString(String raw) {
        if (raw == null) return "0";
        String clean = raw.replaceAll("[^0-9.]", "");
        return clean.isEmpty() ? "0" : clean;
    }

    private static File generateHtmlReport() throws Exception {
        File reportFile = File.createTempFile("battery_report", ".html");
        ProcessBuilder pb = new ProcessBuilder("powercfg", "/batteryreport", "/output", reportFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();
        return reportFile;
    }

    private static String readFileSafe(File file) {
        try {
            String content = Files.readString(file.toPath());
            if (content.contains("\u0000")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] data = new byte[(int) file.length()];
                    fis.read(data);
                    return new String(data, StandardCharsets.UTF_16);
                }
            }
            return content;
        } catch (Exception e) { return ""; }
    }

    public static String getLiveVoltage() {
        try {
            Process process = Runtime.getRuntime().exec("wmic path Win32_Battery get DesignVoltage");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().matches("\\d+")) {
                    double mv = Double.parseDouble(line.trim());
                    return String.format("%.2f V", mv / 1000.0);
                }
            }
        } catch (Exception e) { }
        return "N/A";
    }
}