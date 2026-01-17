package com.cellsense;

import java.util.prefs.Preferences;

public class PreferenceManager {
    private static final Preferences prefs = Preferences.userNodeForPackage(App.class);

    // Keys
    private static final String KEY_ALARM_ACTIVE = "alarm_active";
    private static final String KEY_MAX_LIMIT = "max_limit";
    private static final String KEY_MIN_LIMIT = "min_limit";
    private static final String KEY_MAX_SOUND = "max_sound";
    private static final String KEY_MIN_SOUND = "min_sound";
    private static final String KEY_MAX_LOOP = "max_loop";
    private static final String KEY_MIN_LOOP = "min_loop";

    // --- ALARM ACTIVE ---
    public static boolean isAlarmActive() {
        return prefs.getBoolean(KEY_ALARM_ACTIVE, false);
    }
    public static void setAlarmActive(boolean active) {
        prefs.putBoolean(KEY_ALARM_ACTIVE, active);
    }

    // --- MAX LIMIT ---
    public static double getMaxLimit() {
        return prefs.getDouble(KEY_MAX_LIMIT, 80.0);
    }
    public static void setMaxLimit(double value) {
        prefs.putDouble(KEY_MAX_LIMIT, value);
    }

    // --- MIN LIMIT ---
    public static double getMinLimit() {
        return prefs.getDouble(KEY_MIN_LIMIT, 20.0);
    }
    public static void setMinLimit(double value) {
        prefs.putDouble(KEY_MIN_LIMIT, value);
    }

    // --- SOUND PREFERENCES (NEW) ---
    public static String getMaxSound() {
        return prefs.get(KEY_MAX_SOUND, "Classic Alarm"); // Default
    }
    public static void setMaxSound(String soundName) {
        prefs.put(KEY_MAX_SOUND, soundName);
    }

    public static String getMinSound() {
        return prefs.get(KEY_MIN_SOUND, "Classic Alarm"); // Default
    }
    public static void setMinSound(String soundName) {
        prefs.put(KEY_MIN_SOUND, soundName);
    }
    // --- LOOP PREFERENCES ---
    public static boolean isMaxLoop() {
        return prefs.getBoolean(KEY_MAX_LOOP, true); // Default: Loop ON
    }
    public static void setMaxLoop(boolean loop) {
        prefs.putBoolean(KEY_MAX_LOOP, loop);
    }

    public static boolean isMinLoop() {
        return prefs.getBoolean(KEY_MIN_LOOP, true); // Default: Loop ON
    }
    public static void setMinLoop(boolean loop) {
        prefs.putBoolean(KEY_MIN_LOOP, loop);
    }
}