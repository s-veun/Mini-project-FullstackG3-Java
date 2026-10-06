package org.example.controller;

import org.example.model.Category;
import org.example.service.CategoryService;

import java.util.Scanner;

public final class CategoryController {
    private final CategoryService categories;
    private final Scanner scanner;

    public CategoryController(CategoryService categories, Scanner scanner) {
        this.categories = categories;
        this.scanner = scanner;
    }

    public void showCategories() {
        System.out.println("\n--- CATEGORIES ---");
        for (Category category : categories.getAllCategories())
            System.out.printf("%d | %s | %s%n", category.getCategoryId(), category.getCategoryName(),
                    category.getDescription() == null ? "" : category.getDescription());
    }

    public void handleCategoryMenu() {
        while (true) {
            System.out.println("\n=== CATEGORY MANAGEMENT ===\n1. List\n2. Add\n3. Edit\n4. Delete\n0. Back");
            System.out.print("Choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> showCategories();
                    case "2" -> add();
                    case "3" -> edit();
                    case "4" -> delete();
                    case "0" -> { return; }
                    default -> System.out.println("Choose 1-4 or 0.");
                }
            } catch (RuntimeException e) {
                System.out.println("Unable to complete category action: " + e.getMessage());
            }
        }
    }

    private void add() {
        System.out.print("Name: ");
        String name = scanner.nextLine();
        System.out.print("Description: ");
        String description = scanner.nextLine();
        categories.createCategory(name, description);
        System.out.println("Category created.");
    }

    private void edit() {
        showCategories();
        int id = readId();
        Category current = categories.getCategoryById(id).orElseThrow(() -> new IllegalArgumentException("Category not found."));
        System.out.print("Name [" + current.getCategoryName() + "]: ");
        String name = scanner.nextLine();
        System.out.print("Description [" + current.getDescription() + "]: ");
        String description = scanner.nextLine();
        categories.updateCategory(id, name.isBlank() ? current.getCategoryName() : name,
                description.isBlank() ? current.getDescription() : description);
        System.out.println("Category updated.");
    }

    private void delete() {
        showCategories();
        categories.deleteCategory(readId());
        System.out.println("Category deleted.");
    }

    private int readId() {
        System.out.print("Category ID: ");
        int id = Integer.parseInt(scanner.nextLine().trim());
        if (id <= 0) throw new IllegalArgumentException("ID must be positive.");
        return id;
    }
}
