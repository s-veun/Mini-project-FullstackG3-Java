package org.example.controller;

import org.example.model.Category;
import org.example.service.CategoryService;
import org.example.service.impl.CategoryServiceImpl;
import org.example.utils.SessionManager;

import java.util.Optional;
import java.util.Scanner;

public class CategoryController {
    private final CategoryService categoryService = new CategoryServiceImpl();
    private final Scanner scanner = new Scanner(System.in);

    public void handleCategoryMenu() {
        // ពិនិត្យសិទ្ធិ Admin
        if (SessionManager.getLoggInUser() == null || !SessionManager.getLoggInUser().getRole().equalsIgnoreCase("ADMIN")) {
            System.out.println("Access denied. Admin only.");
            return;
        }

        boolean back = false;
        while (!back) {
            System.out.println("\n=== CATEGORY MANAGEMENT (ADMIN) ===");
            System.out.println("1. View All Categories");
            System.out.println("2. Add New Category");
            System.out.println("3. Update Category");
            System.out.println("4. Delete Category");
            System.out.println("5. Back to Dashboard");
            System.out.print("Choose option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> listCategories();
                case "2" -> addCategory();
                case "3" -> updateCategory();
                case "4" -> deleteCategory();
                case "5" -> back = true;
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void listCategories() {
        System.out.println("\n--- CATEGORY LIST ---");
        for (Category cat : categoryService.getAllCategories()) {
            System.out.println("ID: " + cat.getCategoryId() + " | Name: " + cat.getCategoryName() + " | Desc: " + cat.getDescription());
        }
    }

    private void addCategory() {
        try {
            System.out.print("Category Name: ");
            String name = scanner.nextLine();
            System.out.print("Description: ");
            String desc = scanner.nextLine();

            if (categoryService.createCategory(name, desc)) {
                System.out.println("Category created successfully!");
            } else {
                System.out.println("Failed to create category.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void updateCategory() {
        try {
            listCategories();
            System.out.print("Enter Category ID to update: ");
            int id = Integer.parseInt(scanner.nextLine());

            Optional<Category> existing = categoryService.getCategoryById(id);
            if (existing.isEmpty()) {
                System.out.println("Category ID not found.");
                return;
            }

            System.out.print("New Category Name (" + existing.get().getCategoryName() + "): ");
            String name = scanner.nextLine();
            System.out.print("New Description (" + existing.get().getDescription() + "): ");
            String desc = scanner.nextLine();

            if (categoryService.updateCategory(id, name, desc)) {
                System.out.println("Category updated successfully!");
            } else {
                System.out.println("Update failed.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void deleteCategory() {
        try {
            listCategories();
            System.out.print("Enter Category ID to delete: ");
            int id = Integer.parseInt(scanner.nextLine());

            if (categoryService.deleteCategory(id)) {
                System.out.println("Category deleted successfully!");
            } else {
                System.out.println("Delete failed.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}