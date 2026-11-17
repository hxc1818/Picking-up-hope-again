package com.resilienthope.core.subtitle;

public class RadioSubtitleManager {
    private static String currentSpeaker = "";
    private static String currentText = "";
    private static int displayTicksRemaining = 0;

    public static void postRadioMessage(String speaker, String text, int durationTicks) {
        currentSpeaker = speaker;
        currentText = text;
        displayTicksRemaining = durationTicks;
    }

    public static void clientTick() {
        if (displayTicksRemaining > 0) {
            displayTicksRemaining--;
            if (displayTicksRemaining == 0) {
                currentSpeaker = "";
                currentText = "";
            }
        }
    }

    public static boolean hasActiveDialogue() {
        return displayTicksRemaining > 0 && !currentText.isEmpty();
    }

    public static String getCurrentSpeaker() {
        return currentSpeaker;
    }

    public static String getCurrentText() {
        return currentText;
    }
}
