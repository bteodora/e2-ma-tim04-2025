package com.example.rpgapp.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Notification {
    private String recipientId; // Kome je notifikacija namenjena
    private String title;
    private String message;
    private boolean read;
    @ServerTimestamp
    private Date timestamp;

    public Notification() {}

    public Notification(String recipientId, String title, String message) {
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.read = false;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
