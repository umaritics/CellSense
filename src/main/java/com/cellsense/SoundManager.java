package com.cellsense;

import javafx.scene.media.AudioClip;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SoundManager {
    private static final Map<String, String> SOUND_FILES = new HashMap<>();
    private static AudioClip currentClip;
    private static boolean isAlarmPlaying = false;

    // 1. REGISTER SOUNDS HERE
    static {
        // Name shown in UI  ->  File name in resources
        SOUND_FILES.put("Classic Alarm", "/alarm.mp3");
        SOUND_FILES.put("Gentle Chime",  "/chime.mp3"); // You need to add this file
        SOUND_FILES.put("Future Alert",  "/future.mp3"); // You need to add this file
        SOUND_FILES.put("System Beep",   "beep");       // Always works
    }

    public static Set<String> getSoundNames() {
        return SOUND_FILES.keySet();
    }

    // Play the Loop (Actual Alarm)
    // Updated playAlarm signature to accept loop setting
    public static void playAlarm(String soundName, boolean shouldLoop) {
        if (isAlarmPlaying) return; // Don't restart if already playing

        String fileName = SOUND_FILES.getOrDefault(soundName, "beep");

        if (fileName.equals("beep")) {
            playSystemBeep();
            // System beep can't really loop continuously without a thread,
            // so we just play it once per trigger.
            isAlarmPlaying = true;
            return;
        }

        try {
            if (currentClip == null || !fileName.equals(currentClip.getSource())) {
                loadClip(fileName);
            }
            if (currentClip != null) {
                // THE NEW LOGIC
                if (shouldLoop) {
                    currentClip.setCycleCount(AudioClip.INDEFINITE);
                } else {
                    currentClip.setCycleCount(1);
                }
                currentClip.play();
                isAlarmPlaying = true;
            } else {
                playSystemBeep();
            }
        } catch (Exception e) {
            playSystemBeep();
        }
    }

    // Preview (Play Once)
    public static void previewSound(String soundName) {
        stopAlarm(); // Stop any running alarm first

        String fileName = SOUND_FILES.getOrDefault(soundName, "beep");
        if (fileName.equals("beep")) {
            playSystemBeep();
            return;
        }

        try {
            loadClip(fileName);
            if (currentClip != null) {
                // CHANGE 1: Loop it indefinitely so it doesn't stop immediately
                currentClip.setCycleCount(AudioClip.INDEFINITE);
                currentClip.play();

                // CHANGE 2: Stop it automatically after 3 seconds
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if (currentClip != null && currentClip.isPlaying()) {
                            currentClip.stop();
                        }
                    }
                }, 5000); //milliseconds
            }
        } catch (Exception e) {
            playSystemBeep();
        }
    }
    public static void stopAlarm() {
        isAlarmPlaying = false;
        if (currentClip != null && currentClip.isPlaying()) {
            currentClip.stop();
        }
    }

    private static void loadClip(String fileName) {
        try {
            URL url = SoundManager.class.getResource(fileName);
            if (url != null) {
                currentClip = new AudioClip(url.toExternalForm());
            } else {
                currentClip = null;
            }
        } catch (Exception e) {
            currentClip = null;
        }
    }

    private static void playSystemBeep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
        // Since system beep is instant, we simulate "playing" state for logic
        isAlarmPlaying = true;
    }
}