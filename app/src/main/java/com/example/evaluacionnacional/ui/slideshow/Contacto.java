package com.example.evaluacionnacional.ui.slideshow;

public class Contacto {
    private String name;
    private String email;
    private String photoUrl;

    public Contacto() {
        // Constructor vacío necesario para Firestore
    }

    public Contacto(String name, String email, String photoUrl) {
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
