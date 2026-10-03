package org.example.controller;

import org.example.model.User;
import org.example.service.UserService;
import org.example.utils.SessionManager;

import java.util.Optional;
import java.util.Scanner;

public class AuthController {
    private final UserService userService = new UserService();
    private final Scanner scanner = new Scanner(System.in);

    public void handleAuthMenu() {
        while (!SessionManager.isLoggedIn()) {
            System.out.println("\n=== AUTHENTICATION MENU ===");
            System.out.println("1. Register\n2. Login\n3. Back");
            System.out.print("Choose: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void register() {
        try {
            System.out.print("Username: "); String u = scanner.nextLine();
            System.out.print("Password: "); String p = scanner.nextLine();
            System.out.print("Email: "); String e = scanner.nextLine();
            System.out.print("Role (ADMIN/SELLER/CUSTOMER): "); String r = scanner.nextLine();

            if (userService.register(u, p, e, r)) {
                System.out.println("Registration successful!");
            } else {
                System.out.println("Registration failed.");
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void login() {
        System.out.print("Username: "); String u = scanner.nextLine();
        System.out.print("Password: "); String p = scanner.nextLine();

        Optional<User> user = userService.login(u, p);
        if (user.isPresent()) {
            SessionManager.login(user.get());
            System.out.println("Welcome back, " + user.get().getUsername() + " (" + user.get().getRole() + ")");
        } else {
            System.out.println("Invalid credentials.");
        }
    }
}