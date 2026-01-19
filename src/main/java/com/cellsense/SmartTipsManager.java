package com.cellsense;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SmartTipsManager {

    public static class Tip {
        public String title;
        public String description;
        public String color; // hex code for severity

        public Tip(String title, String description, String color) {
            this.title = title;
            this.description = description;
            this.color = color;
        }
    }

    public static CompletableFuture<List<Tip>> generateTipsAsync() {
        return BatteryReportManager.getBatteryDetailsAsync().thenApply(stats -> {
            List<Tip> tips = new ArrayList<>();

            // 1. Analyze Health
            try {
                double design = parse(stats.get("DesignCapacity"));
                double current = parse(stats.get("FullChargeCapacity"));

                if (design > 0) {
                    double health = (current / design) * 100;
                    if (health < 70) {
                        tips.add(new Tip("Battery Health Critical",
                                "Your battery capacity is below 70%. Consider replacing it soon.", "#f85149")); // Red
                    } else if (health < 85) {
                        tips.add(new Tip("Capacity Degraded",
                                "Your battery has lost " + (int)(100-health) + "% of its original life.", "#e3b341")); // Yellow
                    } else {
                        tips.add(new Tip("Excellent Health",
                                "Your battery is in great condition (" + (int)health + "%). Keep it up!", "#2ea043")); // Green
                    }
                }
            } catch (Exception e) {}

            // 2. Analyze Cycles
            try {
                int cycles = (int) parse(stats.get("CycleCount"));
                if (cycles > 500) {
                    tips.add(new Tip("High Cycle Count",
                            "You have cycled this battery " + cycles + " times. Expect reduced duration.", "#e3b341"));
                }
            } catch (Exception e) {}

            // 3. Analyze Settings (Context Aware)
            if (PreferenceManager.getMaxLimit() == 100) {
                tips.add(new Tip("Optimize Charging",
                        "Limit charging to 80% to extend battery lifespan by 2x.", "#2f81f7")); // Blue
            }

            // 4. General Tip (Fallback)
            if (tips.size() < 2) {
                tips.add(new Tip("Heat is the Enemy",
                        "Avoid using heavy apps while charging to prevent heat damage.", "#8b949e")); // Grey
            }

            return tips;
        });
    }

    private static double parse(String val) {
        if (val == null) return 0;
        return Double.parseDouble(val.replaceAll("[^0-9.]", ""));
    }
}