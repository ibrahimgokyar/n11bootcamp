package com.n11bootcamp.user_service.request;

import java.util.Set;

import jakarta.validation.constraints.*;

public class SignupRequest {

    @NotBlank(message = "Invalid Username: Empty username")
    @Size(min = 3, max = 30, message = "Invalid username: Must be of 3 - 30 characters")
    private String username;

    @NotBlank(message = "Invalid Email: Empty email")
    @Size(max = 50, message = "Invalid email: Must be max 50 characters")
    @Email(message = "Invalid email")
    private String email;

    private Set<String> role;

    @NotBlank(message = "Invalid Password: Empty password")
    @Size(min=6, max = 40, message = "Invalid password: Must be of 6 - 40 characters")
    private String password;



    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRole() {
        return this.role;
    }

    public void setRole(Set<String> role) {
        this.role = role;
    }
}