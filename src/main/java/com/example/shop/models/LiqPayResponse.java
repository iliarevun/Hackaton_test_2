package com.example.shop.models;

import com.example.shop.models.User;
import lombok.Setter;

public class LiqPayResponse {
    @Setter
    private User user;          // Ідентифікатор користувача

    private String os;            // Операційна система
    private boolean success;      // Статус успіху

    // Конструктор
    public LiqPayResponse(User user, String os, boolean success) {
        this.user = user;
        this.os = os;
        this.success = success;
    }

    // Геттери та сеттери
    public User getUser() {
        return user;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "LiqPayResponse{" +
                "user='" + user + '\'' +
                ", os='" + os + '\'' +
                ", success=" + success +
                '}';
    }
}
