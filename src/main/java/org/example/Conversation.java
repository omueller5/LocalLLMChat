package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversation
 * Handles:
 * - Full chat history
 * - Trimmed prompt building
 * - Automatic summarization trigger
 * - Long-term memory storage
 */
public class Conversation {

    private static final int MAX_TURNS_BEFORE_SUMMARY = 12;   // when to summarize
    private static final int MAX_RECENT_TURNS = 8;            // how many to keep after summary

    private final List<String> history = new ArrayList<>();
    private String longTermSummary = "";

    // System instruction so Mochi stops calling herself Claude/ChatGPT/etc.
    private static final String SYSTEM_PROMPT =
            "You are Mochi, a cute, friendly AI assistant running on the user's own computer.\n" +
                    "- Always refer to yourself as \"Mochi\".\n" +
                    "- Never say you are Claude, ChatGPT, Qwen, or any other model name.\n" +
                    "- Answer in a natural, conversational style.\n\n";

    // -----------------------------
    // Add messages
    // -----------------------------

    public void addUser(String text) {
        history.add("User: " + text);
    }

    public void addAssistant(String text) {
        // Use Mochi as the speaker name in the prompt
        history.add("Mochi: " + text);
    }

    // -----------------------------
    // Prompt building
    // -----------------------------

    public String buildTrimmedPrompt() {
        StringBuilder sb = new StringBuilder();

        // System instructions first
        sb.append(SYSTEM_PROMPT);

        if (!longTermSummary.isBlank()) {
            sb.append("Long-term memory about the user:\n");
            sb.append(longTermSummary).append("\n\n");
        }

        int start = Math.max(0, history.size() - MAX_RECENT_TURNS);
        for (int i = start; i < history.size(); i++) {
            sb.append(history.get(i)).append("\n");
        }

        return sb.toString();
    }

    // -----------------------------
    // Summarization logic
    // -----------------------------

    public boolean shouldSummarize() {
        return history.size() >= MAX_TURNS_BEFORE_SUMMARY;
    }

    public String buildSummarizationSource() {
        StringBuilder sb = new StringBuilder();
        for (String msg : history) {
            sb.append(msg).append("\n");
        }
        return sb.toString();
    }

    public void updateLongTermSummary(String summary) {
        this.longTermSummary = summary == null ? "" : summary.trim();
    }

    public void pruneHistoryAfterSummary() {
        if (history.size() <= MAX_RECENT_TURNS) {
            return;
        }

        int start = history.size() - MAX_RECENT_TURNS;
        List<String> recent = new ArrayList<>(history.subList(start, history.size()));
        history.clear();
        history.addAll(recent);
    }

    // -----------------------------
    // Memory Viewer Support
    // -----------------------------

    public String getLongTermSummary() {
        return longTermSummary == null ? "" : longTermSummary;
    }

    public void clearLongTermSummary() {
        longTermSummary = "";
    }

    // Optional: clear everything
    public void clearAllHistory() {
        history.clear();
        longTermSummary = "";
    }
}
