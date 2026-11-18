package com.resilienthope.core.subtitle;

import java.util.ArrayDeque;
import java.util.Queue;

public class RadioSubtitleManager {
    private static class Entry {
        final String speaker;
        final String text;
        final int totalTicks;

        Entry(String speaker, String text, int totalTicks) {
            this.speaker = speaker;
            this.text = text;
            this.totalTicks = Math.max(40, totalTicks);
        }
    }

    private static final Queue<Entry> queue = new ArrayDeque<>();
    private static Entry current = null;
    private static int elapsedTicks = 0;

    public static void postRadioMessage(String speaker, String text, int durationTicks) {
        if (text == null || text.isEmpty()) return;
        Entry entry = new Entry(speaker != null ? speaker : "无线电通讯", text, durationTicks);
        if (current == null) {
            current = entry;
            elapsedTicks = 0;
        } else if (!entry.text.equals(current.text)) {
            queue.offer(entry);
        }
    }

    public static void clientTick() {
        if (current == null) {
            current = queue.poll();
            elapsedTicks = 0;
            return;
        }
        elapsedTicks++;
        if (elapsedTicks >= current.totalTicks) {
            current = queue.poll();
            elapsedTicks = 0;
        }
    }

    public static void clearAll() {
        queue.clear();
        current = null;
        elapsedTicks = 0;
    }

    public static boolean hasActiveDialogue() {
        return current != null && !current.text.isEmpty();
    }

    public static String getCurrentSpeaker() {
        return current != null ? current.speaker : "";
    }

    public static String getCurrentText() {
        if (current == null) return "";
        int charsToShow = Math.min(current.text.length(), Math.max(1, elapsedTicks * 2));
        return current.text.substring(0, charsToShow);
    }

    public static int getQueuedCount() {
        return queue.size();
    }

    public static int getSpeakerColor() {
        if (current == null) return 0xFF38BDF8;
        String sp = current.speaker;
        if (sp.contains("老郑")) return 0xFFFBBF24;
        if (sp.contains("陈风")) return 0xFF4ADE80;
        if (sp.contains("苏晴")) return 0xFF38BDF8;
        if (sp.contains("警报") || sp.contains("紧急")) return 0xFFF87171;
        return 0xFF38BDF8;
    }
}
