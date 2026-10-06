package org.example.controller;

import org.example.model.User;
import org.example.service.UserService;
import org.example.utils.SessionManager;

import java.util.Scanner;

public final class ProfileController {
    private final UserService users;
    private final SessionManager session;
    private final Scanner scanner;

    public ProfileController(UserService users, SessionManager session, Scanner scanner) {
        this.users = users;
        this.session = session;
        this.scanner = scanner;
    }

    public void manageProfile() {
        User user = session.currentUser();
        System.out.printf("%nUsername: %s%nEmail: %s%nRole: %s%nRegistered: %s%n",
                user.getUsername(), user.getEmail(), user.getRole(), user.getCreatedAt());
        System.out.print("New email (Enter to keep current): ");
        String email = scanner.nextLine();
        System.out.print("Current password: ");
        String currentPassword = scanner.nextLine();
        System.out.print("New password (Enter to keep current): ");
        String newPassword = scanner.nextLine();
        try {
            users.updateProfile(email, currentPassword, newPassword);
            System.out.println("Profile updated.");
        } catch (IllegalArgumentException | org.example.exception.AppException e) {
            System.out.println("Profile update failed: " + e.getMessage());
        }
    }
}
