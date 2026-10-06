package org.example.controller;

import org.example.config.Jdbc;
import org.example.dao.*;
import org.example.enums.OrderStatus;
import org.example.exception.AppException;
import org.example.model.*;
import org.example.service.OrderService;
import org.example.utils.SessionManager;

import java.util.List;
import java.util.Scanner;

public final class OrderController {
    private final OrderService orders;
    private final Jdbc jdbc;
    private final CartDao cart;
    private final WishlistDao wishlist;
    private final ReviewDao reviews;
    private final ProductDao products;
    private final PaymentDao payments;
    private final SessionManager session;
    private final Scanner scanner;

    public OrderController(OrderService orders, Jdbc jdbc, CartDao cart, WishlistDao wishlist,
                           ReviewDao reviews, ProductDao products, PaymentDao payments,
                           SessionManager session, Scanner scanner) {
        this.orders = orders;
        this.jdbc = jdbc;
        this.cart = cart;
        this.wishlist = wishlist;
        this.reviews = reviews;
        this.products = products;
        this.payments = payments;
        this.session = session;
        this.scanner = scanner;
    }

    public void customerMenu() {
        while (true) {
            System.out.println("\n=== CUSTOMER SHOPPING ===\n1. View cart\n2. Add to cart\n3. Remove from cart\n" +
                    "4. Wishlist\n5. Reviews\n6. Orders / payments\n0. Back");
            System.out.print("Choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> showCart();
                    case "2" -> addCartItem();
                    case "3" -> removeCartItem();
                    case "4" -> wishlistMenu();
                    case "5" -> reviewMenu();
                    case "6" -> customerOrders();
                    case "0" -> { return; }
                    default -> System.out.println("Choose 1-6 or 0.");
                }
            } catch (RuntimeException e) {
                System.out.println("Action failed: " + e.getMessage());
            }
        }
    }

    public void sellerOrdersMenu() {
        session.require(org.example.enums.Role.SELLER);
        while (true) {
            showOrders();
            System.out.println("\n1. View order details\n0. Back");
            System.out.print("Choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> showDetails(readPositiveInt("Order ID: "));
                    case "0" -> { return; }
                    default -> System.out.println("Choose 1 or 0.");
                }
            } catch (RuntimeException e) {
                System.out.println("Unable to view order: " + e.getMessage());
            }
        }
    }

    public void adminOrdersMenu() {
        session.require(org.example.enums.Role.ADMIN);
        while (true) {
            showOrders();
            System.out.println("\n1. Advance order status\n2. Cancel order\n0. Back");
            System.out.print("Choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> {
                        int id = readPositiveInt("Order ID: ");
                        System.out.print("Next status (SHIPPED/DELIVERED): ");
                        orders.advance(id, OrderStatus.valueOf(scanner.nextLine().trim().toUpperCase()));
                    }
                    case "2" -> orders.cancel(readPositiveInt("Order ID: "));
                    case "0" -> { return; }
                    default -> System.out.println("Choose 1, 2 or 0.");
                }
            } catch (RuntimeException e) {
                System.out.println("Order action failed: " + e.getMessage());
            }
        }
    }

    private void showCart() {
        int userId = session.require(org.example.enums.Role.CUSTOMER).getUserId();
        List<CartItem> items = jdbc.transaction(connection -> cart.find(connection, userId));
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        if (items.isEmpty()) System.out.println("Your cart is empty.");
        for (CartItem item : items) {
            Product product = products.findById(item.getProductId()).orElseThrow(() -> new AppException("Cart product not found."));
            System.out.printf("#%d | %s | quantity %d | $%.2f each%n", product.getProductId(),
                    product.getProductName(), item.getQuantity(), product.getPrice());
            total = total.add(java.math.BigDecimal.valueOf(product.getPrice()).multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
        }
        if (!items.isEmpty()) System.out.println("Cart total: $" + total);
    }

    private void addCartItem() {
        int customerId = session.require(org.example.enums.Role.CUSTOMER).getUserId();
        showCatalogHint();
        int productId = readPositiveInt("Product ID: ");
        int quantity = readPositiveInt("Quantity: ");
        jdbc.transaction(connection -> {
            UserDao.lock(connection, customerId);
            Product product = products.lock(connection, productId)
                    .orElseThrow(() -> new AppException("Product not found."));
            if (!product.isActive() || product.getStockQuantity() < quantity)
                throw new AppException("Product is unavailable or has insufficient stock.");
            List<CartItem> current = cart.find(connection, customerId);
            int oldQuantity = current.stream().filter(i -> i.getProductId() == productId)
                    .mapToInt(CartItem::getQuantity).findFirst().orElse(0);
            if ((long) oldQuantity + quantity > product.getStockQuantity())
                throw new AppException("Requested quantity exceeds available stock.");
            cart.save(connection, customerId, productId, oldQuantity + quantity);
            return null;
        });
        System.out.println("Cart updated.");
    }

    private void removeCartItem() {
        int customerId = session.require(org.example.enums.Role.CUSTOMER).getUserId();
        showCart();
        int productId = readPositiveInt("Product ID to remove: ");
        jdbc.transaction(connection -> {
            UserDao.lock(connection, customerId);
            cart.remove(connection, customerId, productId);
            return null;
        });
        System.out.println("Cart item removed if it was present.");
    }

    private void wishlistMenu() {
        int customerId = session.require(org.example.enums.Role.CUSTOMER).getUserId();
        while (true) {
            System.out.println("\n=== WISHLIST ===\n1. View\n2. Add product\n3. Remove product\n0. Back");
            System.out.print("Choice: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> wishlist.find(customerId).forEach(this::printProduct);
                case "2" -> {
                    int id = readPositiveInt("Product ID: ");
                    if (products.findById(id).isEmpty()) throw new AppException("Product not found.");
                    wishlist.add(customerId, id);
                    System.out.println("Added to wishlist.");
                }
                case "3" -> System.out.println(wishlist.remove(customerId, readPositiveInt("Product ID: "))
                        ? "Removed from wishlist." : "That product was not in the wishlist.");
                case "0" -> { return; }
                default -> System.out.println("Choose 1-3 or 0.");
            }
        }
    }

    private void reviewMenu() {
        int customerId = session.require(org.example.enums.Role.CUSTOMER).getUserId();
        System.out.println("\n1. View product reviews\n2. Write a review\n0. Back");
        System.out.print("Choice: ");
        switch (scanner.nextLine().trim()) {
            case "1" -> {
                int id = readPositiveInt("Product ID: ");
                reviews.findByProduct(id).forEach(r -> System.out.printf("Rating %d/5 | %s%n", r.rating(), r.comment()));
            }
            case "2" -> {
                int productId = readPositiveInt("Purchased product ID: ");
                int rating = readPositiveInt("Rating (1-5): ");
                if (rating > 5) throw new IllegalArgumentException("Rating must be from 1 to 5.");
                System.out.print("Comment: ");
                String comment = scanner.nextLine();
                if (comment.length() > 2000) throw new IllegalArgumentException("Comment cannot exceed 2000 characters.");
                if (!reviews.add(productId, customerId, rating, comment))
                    throw new AppException("A review can only be added for a product from a paid order.");
                System.out.println("Review submitted.");
            }
            case "0" -> { }
            default -> System.out.println("Choose 1, 2 or 0.");
        }
    }

    private void customerOrders() {
        while (true) {
            showOrders();
            System.out.println("\n1. Checkout cart\n2. Pay for pending order\n3. Cancel order\n" +
                    "4. Order details\n5. Payment history\n0. Back");
            System.out.print("Choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> {
                        int id = orders.checkout();
                        System.out.println("Order #" + id + " created as PENDING.");
                    }
                    case "2" -> {
                        int id = readPositiveInt("Order ID: ");
                        System.out.print("Simulate payment result (Y=success, N=failure): ");
                        String paymentResult = scanner.nextLine().trim();
                        if (!paymentResult.equalsIgnoreCase("Y") && !paymentResult.equalsIgnoreCase("N"))
                            throw new IllegalArgumentException("Enter Y or N.");
                        boolean successful = paymentResult.equalsIgnoreCase("Y");
                        orders.pay(id, successful);
                        System.out.println(successful ? "Payment successful; order marked PAID." : "Payment failed; order remains PENDING.");
                    }
                    case "3" -> orders.cancel(readPositiveInt("Order ID: "));
                    case "4" -> showDetails(readPositiveInt("Order ID: "));
                    case "5" -> showPaymentHistory();
                    case "0" -> { return; }
                    default -> System.out.println("Choose 1-5 or 0.");
                }
            } catch (RuntimeException e) {
                System.out.println("Order action failed: " + e.getMessage());
            }
        }
    }

    private void showOrders() {
        List<Order> history = orders.history();
        if (history.isEmpty()) System.out.println("No orders found.");
        for (Order order : history)
            System.out.printf("#%d | %s | $%.2f | %s%n", order.getOrderId(), order.getOrderDate(),
                    order.getTotalAmount(), order.getOrderStatus());
    }

    private void showDetails(int orderId) {
        Order order = orders.find(orderId);
        System.out.printf("Order #%d | %s | $%.2f%n", order.getOrderId(), order.getOrderStatus(), order.getTotalAmount());
        for (OrderDetail detail : orders.details(orderId))
            System.out.printf("%s x%d | $%.2f%n", detail.productName(), detail.quantity(), detail.subtotal());
    }

    private void showPaymentHistory() {
        int customerId = session.require(org.example.enums.Role.CUSTOMER).getUserId();
        List<PaymentDao.PaymentRecord> history = payments.historyForCustomer(customerId);
        if (history.isEmpty()) System.out.println("No payment transactions found.");
        for (PaymentDao.PaymentRecord payment : history)
            System.out.printf("Payment #%d | Order #%d | %s | $%s | %s%n",
                    payment.paymentId(), payment.orderId(), payment.status(), payment.amount(), payment.date());
    }

    private void showCatalogHint() {
        System.out.println("Browse catalog from the dashboard to see available product IDs.");
    }

    private void printProduct(Product product) {
        System.out.printf("#%d | %s | $%.2f | %s%n", product.getProductId(), product.getProductName(),
                product.getPrice(), product.isActive() ? "ACTIVE" : "INACTIVE");
    }

    private int readPositiveInt(String prompt) {
        System.out.print(prompt);
        int value = Integer.parseInt(scanner.nextLine().trim());
        if (value <= 0) throw new IllegalArgumentException("Enter a positive number.");
        return value;
    }
}
