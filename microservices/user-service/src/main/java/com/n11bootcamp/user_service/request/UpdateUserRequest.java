package com.n11bootcamp.user_service.request;

import jakarta.validation.constraints.*;

public class UpdateUserRequest {

    @Size(min = 6, max = 40, message = "Invalid password: Must be of 6 - 40 characters")
    private String password;

    @Size(max = 50, message = "Invalid email: Must be max 50 characters")
    @Email(message = "Invalid email")
    private String email;

    // Getters and setters

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}