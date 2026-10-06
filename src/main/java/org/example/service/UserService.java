package org.example.service;

import org.example.model.User;

import java.util.Optional;

public interface UserService {
    User register(String username, String password, String email, String role);
    Optional<User> login(String username, String password);
    void updateProfile(String email, String currentPassword, String newPassword);
}
