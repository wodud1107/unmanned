package com.example.solace.util;

public class AlertEntry {
    private final String text;
    private final String originalTs;
    private boolean read = false;
    private boolean blinkState = false;

    public AlertEntry(String text, String originalTs) {
        this.text = text;
        this.originalTs = originalTs;
    }

    public AlertEntry(String text, java.time.LocalTime time) {
        this.text = text;
        this.originalTs = time.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public String getText() {
        return text;
    }

    public String getOriginalTs() {
        return originalTs;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isBlinkState() {
        return blinkState;
    }

    public void setBlinkState(boolean blinkState) {
        this.blinkState = blinkState;
    }
}