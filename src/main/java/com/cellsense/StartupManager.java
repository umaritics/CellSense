package com.cellsense;

import java.io.File;

public class StartupManager {

    public static void toggleStartup(boolean enable) {
        String appName = "CellSense";
        String startupPath = System.getProperty("user.home") +
                "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\" +
                appName + ".lnk";

        if (enable) {
            createShortcut(startupPath);
        } else {
            deleteShortcut(startupPath);
        }
    }

    private static void createShortcut(String startupPath) {
        String runtimePath = System.getProperty("java.home");
        // Get the folder containing the exe (e.g., C:\Users\You\AppData\Local\CellSense)
        String appDir = new File(runtimePath).getParent();
        String exePath = appDir + File.separator + "CellSense.exe";

        // PowerShell script: Now sets the WorkingDirectory!
        String script = "$s=(New-Object -COM WScript.Shell).CreateShortcut('" + startupPath + "');" +
                "$s.TargetPath='" + exePath + "';" +
                "$s.Arguments='--minimized';" +
                "$s.WorkingDirectory='" + appDir + "';" +
                "$s.Save()";

        try {
            String[] cmd = {"powershell", "-Command", script};
            Runtime.getRuntime().exec(cmd);
            System.out.println("Startup ENABLED. Shortcut created at: " + startupPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteShortcut(String startupPath) {
        File shortcut = new File(startupPath);
        if (shortcut.exists()) {
            shortcut.delete();
            System.out.println("Startup DISABLED. Shortcut removed.");
        }
    }
    // Add this inside StartupManager class
    public static boolean isStartupEnabled() {
        String appName = "CellSense";
        String startupPath = System.getProperty("user.home") +
                "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\" +
                appName + ".lnk";
        return new File(startupPath).exists();
    }
}