package org.example.controller;

import org.example.model.User;
import org.example.service.UserService;
import org.example.utils.SessionManager;

import java.util.Optional;
import java.util.Scanner;

public final class AuthController {
    private final UserService users;
    private final SessionManager session;
    private final Scanner scanner;

    public AuthController(UserService users, SessionManager session, Scanner scanner) {
        this.users = users;
        this.session = session;
        this.scanner = scanner;
    }

    public void handleAuthMenu() {
        while (!session.isLoggedIn()) {
            System.out.println("\n=== AUTHENTICATION ===\n1. Register\n2. Login\n0. Back");
            System.out.print("Choice: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> register();
                case "2" -> login();
                case "0" -> { return; }
                default -> System.out.println("Choose 1, 2 or 0.");
            }
        }
    }

    private void register() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password (at least 5 characters): ");
        String password = scanner.nextLine();
        System.out.print("Account type (CUSTOMER/SELLER): ");
        String role = scanner.nextLine();
        try {
            User user = users.register(username, password, email, role);
            System.out.println("Registered " + user.getUsername() + " as " + user.getRole() + ". Please log in.");
        } catch (IllegalArgumentException | org.example.exception.AppException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private void login() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        Optional<User> user = users.login(username, password);
        if (user.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }
        session.login(user.get());
        System.out.println("Welcome, " + user.get().getUsername() + " (" + user.get().getRole() + ").");
    }
}
