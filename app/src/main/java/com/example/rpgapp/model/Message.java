package com.example.rpgapp.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {

    private String messageId;
    private String text;
    private String senderId;
    private String senderUsername;
    @ServerTimestamp
    private Date timestamp;

    public Message() {}

    public Message(String text, String senderId, String senderUsername) {
        this.text = text;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}