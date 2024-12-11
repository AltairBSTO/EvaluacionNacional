package com.example.evaluacionnacional.ui.slideshow;

public class Contacto {
    private String name;
    private String username; // Cambiar email por username
    private String photoUrl;

    public Contacto() {
        // Constructor vacío necesario para Firestore
    }

    public Contacto(String name, String username, String photoUrl) {
        this.name = name;
        this.username = username;
        this.photoUrl = photoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
