package org.example.utils;

import org.example.enums.Role;
import org.example.exception.AppException;
import org.example.model.User;

import java.util.Arrays;
import java.util.Objects;

public class SessionManager {
    private User loggedInUser;

    public void login(User user) {
        loggedInUser = Objects.requireNonNull(user, "user");
    }

    public void logout() {
        loggedInUser = null;
    }

    public User currentUser() {
        if (loggedInUser == null) throw new AppException("Please log in first.");
        return loggedInUser;
    }

    public User require(Role... allowedRoles) {
        User user = currentUser();
        Role role;
        try {
            role = Role.valueOf(user.getRole().toUpperCase());
        } catch (RuntimeException e) {
            throw new AppException("The current account has an invalid role.", e);
        }
        if (Arrays.stream(allowedRoles).noneMatch(role::equals)) {
            throw new AppException("You do not have permission to perform this action.");
        }
        return user;
    }

    public boolean isLoggedIn() {
        return loggedInUser != null;
    }
}
