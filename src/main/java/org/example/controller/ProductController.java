package org.example.controller;

import org.example.model.Product;
import org.example.service.ProductService;
import org.example.utils.SessionManager;
import org.example.utils.ProductImportReader;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public final class ProductController {
    private final ProductService products;
    private final CategoryController categories;
    private final SessionManager session;
    private final Scanner scanner;

    public ProductController(ProductService products, CategoryController categories,
                             SessionManager session, Scanner scanner) {
        this.products = products;
        this.categories = categories;
        this.session = session;
        this.scanner = scanner;
    }

    public void showCatalog() {
        showCatalog(products.getCatalog());
        System.out.println("1. Search/filter catalog  0. Back");
        System.out.print("Choice: ");
        if (scanner.nextLine().trim().equals("1")) searchCatalog();
    }

    private void showCatalog(List<Product> catalog) {
        System.out.println("\n=== PRODUCT CATALOG ===");
        if (catalog.isEmpty()) System.out.println("No products are currently available.");
        for (Product p : catalog) print(p);
    }

    private void searchCatalog() {
        System.out.print("Search text (Enter to skip): ");
        String term = scanner.nextLine();
        System.out.print("Category ID (Enter to skip): ");
        String categoryValue = scanner.nextLine().trim();
        System.out.print("Minimum price (Enter to skip): ");
        String minValue = scanner.nextLine().trim();
        System.out.print("Maximum price (Enter to skip): ");
        String maxValue = scanner.nextLine().trim();
        Integer categoryId = categoryValue.isEmpty() ? null : Integer.valueOf(categoryValue);
        BigDecimal minPrice = minValue.isEmpty() ? null : new BigDecimal(minValue);
        BigDecimal maxPrice = maxValue.isEmpty() ? null : new BigDecimal(maxValue);
        showCatalog(products.searchCatalog(term, categoryId, minPrice, maxPrice));
    }

    public void handleProductMenu() {
        while (true) {
            System.out.println("\n=== PRODUCT MANAGEMENT ===\n1. List products\n2. Add product\n3. Edit product\n" +
                    "4. Change product availability\n5. Import products (CSV/XLSX)\n0. Back");
            System.out.print("Choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> products.getMyProducts().forEach(this::print);
                    case "2" -> addProduct();
                    case "3" -> editProduct();
                    case "4" -> toggleProduct();
                    case "5" -> importProducts();
                    case "0" -> { return; }
                    default -> System.out.println("Choose 1-5 or 0.");
                }
            } catch (RuntimeException e) {
                System.out.println("Unable to complete product action: " + e.getMessage());
            }
        }
    }

    private void addProduct() {
        categories.showCategories();
        boolean admin = session.currentUser().getRole().equalsIgnoreCase("ADMIN");
        int sellerId = admin ? readInt("Seller user ID: ") : session.currentUser().getUserId();
        int categoryId = readInt("Category ID: ");
        System.out.print("Product name: ");
        String name = scanner.nextLine();
        System.out.print("Description: ");
        String description = scanner.nextLine();
        double price = readDouble("Price: ");
        int stock = readInt("Stock quantity: ");
        if (admin) products.addProductForSeller(sellerId, categoryId, name, description, price, stock);
        else products.addProduct(categoryId, name, description, price, stock);
        System.out.println("Product added.");
    }

    private void editProduct() {
        products.getMyProducts().forEach(this::print);
        int id = readInt("Product ID: ");
        Product current = products.find(id).orElseThrow(() -> new IllegalArgumentException("Product not found."));
        System.out.print("Category ID [" + current.getCategoryId() + "]: ");
        String category = scanner.nextLine().trim();
        System.out.print("Name [" + current.getProductName() + "]: ");
        String name = scanner.nextLine();
        System.out.print("Description [" + current.getDescription() + "]: ");
        String description = scanner.nextLine();
        System.out.print("Price [" + current.getPrice() + "]: ");
        String price = scanner.nextLine().trim();
        System.out.print("Stock [" + current.getStockQuantity() + "]: ");
        String stock = scanner.nextLine().trim();
        products.updateProduct(id, category.isEmpty() ? current.getCategoryId() : Integer.parseInt(category),
                name.isBlank() ? current.getProductName() : name,
                description.isBlank() ? current.getDescription() : description,
                price.isEmpty() ? current.getPrice() : Double.parseDouble(price),
                stock.isEmpty() ? current.getStockQuantity() : Integer.parseInt(stock));
        System.out.println("Product updated.");
    }

    private void toggleProduct() {
        products.getMyProducts().forEach(this::print);
        int id = readInt("Product ID: ");
        Product product = products.find(id).orElseThrow(() -> new IllegalArgumentException("Product not found."));
        products.setProductActive(id, !product.isActive());
        System.out.println(product.isActive() ? "Product deactivated." : "Product activated.");
    }

    private void importProducts() {
        boolean admin = session.currentUser().getRole().equalsIgnoreCase("ADMIN");
        Integer sellerId = admin ? readInt("Seller user ID for imported products: ") : null;
        System.out.print("Import file path (CSV/XLSX): ");
        Path path = Path.of(scanner.nextLine().trim()).toAbsolutePath().normalize();
        List<ProductService.ProductImport> imports = ProductImportReader.read(path);
        int imported = products.importProducts(sellerId, imports);
        System.out.println("Imported " + imported + " products.");
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        return Integer.parseInt(scanner.nextLine().trim());
    }

    private double readDouble(String prompt) {
        System.out.print(prompt);
        return Double.parseDouble(scanner.nextLine().trim());
    }

    private void print(Product p) {
        System.out.printf("#%d | %s | $%.2f | stock %d | %s%n", p.getProductId(), p.getProductName(),
                p.getPrice(), p.getStockQuantity(), p.isActive() ? "ACTIVE" : "INACTIVE");
    }
}
