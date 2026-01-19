package com.cellsense;

import javafx.scene.media.AudioClip;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class SoundManager {
    private static final Map<String, String> SOUND_FILES = new HashMap<>();
    private static AudioClip currentClip;
    private static boolean isAlarmPlaying = false;

    // NEW: Protects the preview from being killed by the battery checker
    private static boolean isPreviewing = false;
    private static Timer previewTimer;

    static {
        SOUND_FILES.put("Classic Alarm", "/alarm.mp3");
        SOUND_FILES.put("Gentle Chime",  "/chime.mp3");
        SOUND_FILES.put("Future Alert",  "/future.mp3");
        SOUND_FILES.put("System Beep",   "beep");
    }

    public static Set<String> getSoundNames() {
        return SOUND_FILES.keySet();
    }

    // --- PLAY ALARM (Called by Battery Checker) ---
    public static void playAlarm(String soundName, boolean shouldLoop) {
        // If user is previewing a sound, don't interrupt them with the alarm logic yet
        if (isPreviewing) return;

        if (isAlarmPlaying) return;

        startSound(soundName, shouldLoop ? AudioClip.INDEFINITE : 1);
        isAlarmPlaying = true;
    }

    // --- PREVIEW SOUND (Called by User) ---
    public static void previewSound(String soundName) {
        // Force stop everything to start fresh
        forceStop();

        isPreviewing = true; // Raise the flag!
        startSound(soundName, AudioClip.INDEFINITE); // Loop it so it lasts 5 seconds

        // Kill it after 5 seconds
        previewTimer = new Timer();
        previewTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                forceStop(); // Timer done, kill sound
            }
        }, 5000);
    }

    // --- STOP ALARM (Called by Battery Checker) ---
    public static void stopAlarm() {
        // CRITICAL FIX: If we are previewing, IGNORE the battery checker's request to stop.
        if (isPreviewing) return;

        forceStop();
    }

    // Internal helper to actually stop sound
    private static void forceStop() {
        isAlarmPlaying = false;
        isPreviewing = false; // Lower the flag

        if (previewTimer != null) {
            previewTimer.cancel();
            previewTimer = null;
        }

        if (currentClip != null && currentClip.isPlaying()) {
            currentClip.stop();
        }
    }

    private static void startSound(String soundName, int cycleCount) {
        String fileName = SOUND_FILES.getOrDefault(soundName, "beep");

        if (fileName.equals("beep")) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            return;
        }

        try {
            // Reload if different sound
            if (currentClip == null || !fileName.equals(currentClip.getSource())) {
                URL url = SoundManager.class.getResource(fileName);
                if (url != null) {
                    currentClip = new AudioClip(url.toExternalForm());
                }
            }

            if (currentClip != null) {
                currentClip.setCycleCount(cycleCount);
                currentClip.play();
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception e) {
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }
}