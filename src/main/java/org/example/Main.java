package org.example;

import org.example.config.DatabaseConnection;
import org.example.config.Jdbc;
import org.example.controller.*;
import org.example.dao.*;
import org.example.enums.Role;
import org.example.exception.AppException;
import org.example.model.User;
import org.example.service.*;
import org.example.service.impl.*;
import org.example.utils.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try (Connection ignored = DatabaseConnection.getConnection()) {
            // Verify configuration before presenting the CLI.
        } catch (SQLException e) {
            System.err.println("Unable to start the marketplace: " + e.getMessage());
            System.err.println("Set DB_URL, DB_USER and DB_PASSWORD, then ensure PostgreSQL is available.");
            return;
        }
        try (Scanner scanner = new Scanner(System.in)) {
            run(scanner);
        } catch (RuntimeException e) {
            System.err.println("Marketplace stopped: " + e.getMessage());
        }
    }

    private static void run(Scanner scanner) {
        SessionManager session = new SessionManager();
        Jdbc jdbc = new Jdbc();
        UserDao userDao = new UserDao();
        ProductDao productDao = new ProductDao();
        CategoryDao categoryDao = new CategoryDao();
        CartDao cartDao = new CartDao();
        OrderDao orderDao = new OrderDao();
        PaymentDao paymentDao = new PaymentDao();
        WishlistDao wishlistDao = new WishlistDao();
        ReviewDao reviewDao = new ReviewDao();
        ReportDao reportDao = new ReportDao();

        UserService userService = new UserServiceImpl(userDao, session);
        ProductService productService = new ProductServiceImpl(productDao, categoryDao, userDao, session, jdbc);
        CategoryService categoryService = new CategoryServiceImpl(categoryDao, session);
        OrderService orderService = new OrderServiceImpl(jdbc, orderDao, cartDao, productDao, paymentDao, session);

        AuthController auth = new AuthController(userService, session, scanner);
        CategoryController categories = new CategoryController(categoryService, scanner);
        ProductController products = new ProductController(productService, categories, session, scanner);
        OrderController orders = new OrderController(orderService, jdbc, cartDao, wishlistDao, reviewDao,
                productDao, paymentDao, session, scanner);
        ProfileController profile = new ProfileController(userService, session, scanner);
        AdminController admin = new AdminController(reportDao, categories, products, orders, profile, session, scanner);

        while (true) {
            if (session.isLoggedIn()) {
                dispatch(session.currentUser(), session, products, orders, admin, profile, scanner);
                continue;
            }
            System.out.println("\n=== JAVA JDBC E-COMMERCE MARKETPLACE ===\n1. Register / Login\n2. Browse products\n0. Exit");
            System.out.print("Choice: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> auth.handleAuthMenu();
                case "2" -> products.showCatalog();
                case "0" -> {
                    System.out.println("Goodbye.");
                    return;
                }
                default -> System.out.println("Choose 1, 2 or 0.");
            }
        }
    }

    private static void dispatch(User user, SessionManager session, ProductController products,
                                 OrderController orders, AdminController admin, ProfileController profile, Scanner scanner) {
        String role = user.getRole().toUpperCase();
        if (role.equals(Role.ADMIN.name())) {
            admin.runAdminMenu();
            return;
        }
        if (role.equals(Role.CUSTOMER.name())) {
            System.out.println("\n=== CUSTOMER DASHBOARD: " + user.getUsername() + " ===\n" +
                    "1. Browse catalog\n2. Cart / Wishlist / Reviews / Orders\n3. Profile\n0. Logout");
            System.out.print("Choice: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> products.showCatalog();
                case "2" -> orders.customerMenu();
                case "3" -> profile.manageProfile();
                case "0" -> { session.logout(); System.out.println("Logged out."); }
                default -> System.out.println("Choose 1-3 or 0.");
            }
            return;
        }
        if (role.equals(Role.SELLER.name())) {
            System.out.println("\n=== SELLER DASHBOARD: " + user.getUsername() + " ===\n" +
                    "1. Browse catalog\n2. Manage my products\n3. My orders\n4. Sales report\n5. Profile\n0. Logout");
            System.out.print("Choice: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> products.showCatalog();
                case "2" -> products.handleProductMenu();
                case "3" -> orders.sellerOrdersMenu();
                case "4" -> admin.runSellerReports();
                case "5" -> admin.manageProfile();
                case "0" -> { session.logout(); System.out.println("Logged out."); }
                default -> System.out.println("Choose 1-5 or 0.");
            }
            return;
        }
        session.logout();
        throw new AppException("Unknown account role. Session ended.");
    }

}
