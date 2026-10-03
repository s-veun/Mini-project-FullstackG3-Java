package org.example.controller;

import org.example.model.Product;
import org.example.service.ProductService;
import org.example.utils.SessionManager;

import java.util.Scanner;

public class ProductController {
    private final ProductService productService = new ProductService();
    private final Scanner scanner = new Scanner(System.in);

    public void showCatalog() {
        System.out.println("\n=== PRODUCT CATALOG ===");
        for (Product p : productService.getAllProducts()) {
            System.out.println("ID: " + p.getProductId() + " | Name: " + p.getProductName() + " | Price: $" + p.getPrice() + " | Stock: " + p.getStockQuantity());
        }
    }

    public void addNewProduct() {
        if (SessionManager.getLoggInUser() == null || !SessionManager.getLoggInUser().getRole().equalsIgnoreCase("SELLER")) {
            System.out.println("Access denied. Sellers only.");
            return;
        }
        try {
            System.out.print("Category ID: "); int catId = Integer.parseInt(scanner.nextLine());
            System.out.print("Product Name: "); String name = scanner.nextLine();
            System.out.print("Description: "); String desc = scanner.nextLine();
            System.out.print("Price: "); double price = Double.parseDouble(scanner.nextLine());
            System.out.print("Stock Quantity: "); int stock = Integer.parseInt(scanner.nextLine());

            boolean success = productService.addProduct(SessionManager.getLoggInUser().getUserId(), catId, name, desc, price, stock);
            if (success) System.out.println("Product added successfully!");
        } catch (Exception e) {
            System.out.println("Error adding product: " + e.getMessage());
        }
    }
}