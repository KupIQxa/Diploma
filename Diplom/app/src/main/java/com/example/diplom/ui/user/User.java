package com.example.diplom.ui.user;

public class User {
    private String fullName;
    private String email;

    // Конструктор для инициализации данных пользователя
    public User(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    // Геттер для полного имени
    public String getFullName() {
        return fullName;
    }

    // Сеттер для полного имени
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    // Геттер для электронной почты
    public String getEmail() {
        return email;
    }

    // Сеттер для электронной почты
    public void setEmail(String email) {
        this.email = email;
    }
}
