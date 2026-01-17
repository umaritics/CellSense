package com.cellsense;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class BatteryManager {

    // Returns the battery percentage (0-100)
    public static int getBatteryLevel() {
        try {
            // Windows command to get battery %
            Process process = Runtime.getRuntime().exec("wmic path Win32_Battery get EstimatedChargeRemaining");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().matches("\\d+")) {
                    return Integer.parseInt(line.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0; // Default if error
    }

    // Returns true if plugged in, false if on battery
    public static boolean isPluggedIn() {
        try {
            // Windows command to get power status (2 = AC Connected)
            Process process = Runtime.getRuntime().exec("wmic path Win32_Battery get BatteryStatus");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().matches("\\d+")) {
                    int status = Integer.parseInt(line.trim());
                    return status == 2;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}