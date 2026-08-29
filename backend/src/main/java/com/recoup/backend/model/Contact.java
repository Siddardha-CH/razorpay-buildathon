package com.recoup.backend.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Contact {

    private String phone;
    private String email;
    private boolean whatsappOptIn;
    private boolean dnd;

    public Contact() {
    }

    public Contact(String phone, String email, boolean whatsappOptIn, boolean dnd) {
        this.phone = phone;
        this.email = email;
        this.whatsappOptIn = whatsappOptIn;
        this.dnd = dnd;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public boolean isWhatsappOptIn() {
        return whatsappOptIn;
    }

    public boolean isDnd() {
        return dnd;
    }
}
