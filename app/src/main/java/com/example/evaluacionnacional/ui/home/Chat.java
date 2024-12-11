package com.example.evaluacionnacional.ui.home;

public class Chat {
    private String partnerName;
    private String topic;
    private String currentUserEmail;
    private String partnerEmail;

    // Constructor
    public Chat(String partnerName, String topic, String currentUserEmail, String partnerEmail) {
        this.partnerName = partnerName;
        this.topic = topic;
        this.currentUserEmail = currentUserEmail;
        this.partnerEmail = partnerEmail;
    }

    // Getters y Setters
    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public void setCurrentUserEmail(String currentUserEmail) {
        this.currentUserEmail = currentUserEmail;
    }

    public String getPartnerEmail() {
        return partnerEmail;
    }

    public void setPartnerEmail(String partnerEmail) {
        this.partnerEmail = partnerEmail;
    }
}
