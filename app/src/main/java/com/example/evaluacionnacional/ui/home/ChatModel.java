package com.example.evaluacionnacional.ui.home;

public class ChatModel {

    private String messageContent;
    private String senderEmail;
    private String timestamp;

    public ChatModel(String messageContent, String senderEmail, String timestamp) {
        this.messageContent = messageContent;
        this.senderEmail = senderEmail;
        this.timestamp = timestamp;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getTimestamp() {
        return timestamp;
    }


}
