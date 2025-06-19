package com.example.solace.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AlertEntry {
    private final String text;
    private final String originalTs;
    private boolean read = false;
    private boolean blinkState = false;

    public AlertEntry(String text, String originalTs) {
        this.text = text;
        this.originalTs = originalTs;
    }

    public AlertEntry(String text, LocalDateTime time) {
        this.text = text;
        this.originalTs = time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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