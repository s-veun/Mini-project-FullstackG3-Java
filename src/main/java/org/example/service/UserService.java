package org.example.service;

import org.example.dao.UserDao;
import org.example.model.User;
import org.example.utils.InputValidator;

import java.util.Optional;

public class UserService {
    private final UserDao userDao = new UserDao();

    public boolean register(String username, String password, String email, String role) {
        if (!InputValidator.isValidString(username) || !InputValidator.isValidString(password) || !InputValidator.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid input data provided.");
        }
        User user = User.builder()
                .username(username)
                .password(password)
                .email(email)
                .role(role.toUpperCase())
                .build();
        return userDao.save(user);
    }

    public Optional<User> login(String username, String password) {
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return userOpt;
        }
        return Optional.empty();
    }
}