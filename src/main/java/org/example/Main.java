package org.example;

import org.example.controller.AuthController;
import org.example.controller.ProductController;
import org.example.controller.AdminController;
import org.example.controller.CategoryController;
import org.example.utils.SessionManager;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        AuthController authController = new AuthController();
        ProductController productController = new ProductController();
        AdminController adminController = new AdminController();
        CategoryController categoryController = new CategoryController();

        System.out.println("=== JAVA JDBC E-COMMERCE MARKETPLACE ===");
        boolean running = true;

        while (running) {
            if (!SessionManager.isLoggedIn()) {
                System.out.println("\n1. Authentication\n2. Browse Products\n3. Exit");
                System.out.print("Choice: ");
                String choice = scanner.nextLine();
                switch (choice) {
                    case "1" -> authController.handleAuthMenu();
                    case "2" -> productController.showCatalog();
                    case "3" -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            } else {
                String role = SessionManager.getLoggInUser().getRole();
                System.out.println("\n--- Dashboard (" + role + ") ---");

                if (role.equalsIgnoreCase("ADMIN")) {
                    System.out.println("1. View Products");
                    System.out.println("2. Manage Categories (Add/View/Update/Delete)");
                    System.out.println("3. View Reports");
                    System.out.println("4. Logout");
                    System.out.print("Choice: ");
                    String choice = scanner.nextLine();
                    switch (choice) {
                        case "1" -> productController.showCatalog();
                        case "2" -> categoryController.handleCategoryMenu();
                        case "3" -> adminController.displayReports();
                        case "4" -> {
                            SessionManager.logout();
                            System.out.println("Logged out successfully.");
                        }
                        default -> System.out.println("Invalid choice.");
                    }
                } else if (role.equalsIgnoreCase("SELLER")) {
                    // Menu សម្រាប់ SELLER
                    System.out.println("1. View Products");
                    System.out.println("2. Add Product");
                    System.out.println("3. Logout");
                    System.out.print("Choice: ");
                    String choice = scanner.nextLine();
                    switch (choice) {
                        case "1" -> productController.showCatalog();
                        case "2" -> productController.addNewProduct();
                        case "3" -> {
                            org.example.utils.SessionManager.logout();
                            System.out.println("Logged out successfully.");
                        }
                        default -> System.out.println("Invalid choice.");
                    }
                } else {
                    // Menu សម្រាប់ CUSTOMER
                    System.out.println("1. View Products");
                    System.out.println("2. Logout");
                    System.out.print("Choice: ");
                    String choice = scanner.nextLine();
                    switch (choice) {
                        case "1" -> productController.showCatalog();
                        case "2" -> {
                            org.example.utils.SessionManager.logout();
                            System.out.println("Logged out successfully.");
                        }
                        default -> System.out.println("Invalid choice.");
                    }
                }
            }
        }
        System.out.println("Application closed.");
    }
}