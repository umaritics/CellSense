package com.cellsense;

import javafx.scene.media.AudioClip;
import java.net.URL;

public class SoundManager {
    private static AudioClip alarmClip;
    private static boolean isPlaying = false;

    // Load the sound file (we will add this file later)
    static {
        try {
            // Look for "alarm.mp3" or "alarm.wav" in the resources folder
            URL soundURL = SoundManager.class.getResource("/alarm.mp3");
            if (soundURL != null) {
                alarmClip = new AudioClip(soundURL.toExternalForm());
                alarmClip.setCycleCount(AudioClip.INDEFINITE); // Loop forever
            }
        } catch (Exception e) {
            System.out.println("No custom sound found. Using system beep.");
        }
    }

    public static void playAlarm() {
        if (isPlaying) return; // Don't play if already playing

        isPlaying = true;
        if (alarmClip != null) {
            alarmClip.play();
        } else {
            // Fallback if no file exists
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    public static void stopAlarm() {
        if (!isPlaying) return;

        isPlaying = false;
        if (alarmClip != null) {
            alarmClip.stop();
        }
    }
}