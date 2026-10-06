package org.example.service.impl;

import org.example.config.Jdbc;
import org.example.dao.*;
import org.example.enums.OrderStatus;
import org.example.enums.PaymentStatus;
import org.example.enums.Role;
import org.example.exception.AppException;
import org.example.model.*;
import org.example.service.OrderService;
import org.example.utils.SessionManager;

import java.math.BigDecimal;
import java.util.*;

public final class OrderServiceImpl implements OrderService {
    private final Jdbc jdbc;
    private final OrderDao orders;
    private final CartDao cart;
    private final ProductDao products;
    private final PaymentDao payments;
    private final SessionManager session;

    public OrderServiceImpl(Jdbc jdbc, OrderDao orders, CartDao cart, ProductDao products,
                            PaymentDao payments, SessionManager session) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.cart = cart;
        this.products = products;
        this.payments = payments;
        this.session = session;
    }

    @Override
    public int checkout() {
        int customer = session.require(Role.CUSTOMER).getUserId();
        return jdbc.transaction(connection -> {
            UserDao.lock(connection, customer);
            List<CartItem> items = cart.find(connection, customer);
            if (items.isEmpty()) throw new AppException("Cart is empty.");
            BigDecimal total = BigDecimal.ZERO;
            Map<Integer, Product> lockedProducts = new LinkedHashMap<>();
            for (CartItem item : items) {
                if (item.getQuantity() <= 0) throw new AppException("Cart contains an invalid quantity.");
                Product product = products.lock(connection, item.getProductId())
                        .orElseThrow(() -> new AppException("A cart product no longer exists."));
                if (!product.isActive() || product.getStockQuantity() < item.getQuantity())
                    throw new AppException("Insufficient stock for " + product.getProductName() + ". Your cart was kept.");
                lockedProducts.put(product.getProductId(), product);
                total = total.add(BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            if (total.compareTo(new BigDecimal("999999999999.99")) > 0)
                throw new AppException("Order total exceeds the supported limit.");
            int orderId = orders.create(connection, customer, total);
            for (CartItem item : items) {
                Product product = lockedProducts.get(item.getProductId());
                orders.addDetail(connection, orderId, product, item.getQuantity());
                products.reserve(connection, product.getProductId(), item.getQuantity());
            }
            cart.clear(connection, customer);
            return orderId;
        });
    }

    @Override
    public void pay(int orderId, boolean successful) {
        int customerId = session.require(Role.CUSTOMER).getUserId();
        jdbc.transaction(connection -> {
            Order order = orders.lock(connection, orderId).orElseThrow(() -> new AppException("Order not found."));
            if (order.getCustomerId() != customerId) throw new AppException("You can only pay for your own order.");
            if (!OrderStatus.PENDING.name().equals(order.getOrderStatus()))
                throw new AppException("Only pending orders can be paid.");
            if (payments.latestStatus(connection, orderId).orElse(null) == PaymentStatus.SUCCESS)
                throw new AppException("This order has already been paid.");
            PaymentStatus status = successful ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            payments.record(connection, orderId, BigDecimal.valueOf(order.getTotalAmount()), status);
            if (successful) orders.status(connection, orderId, OrderStatus.PAID);
            return null;
        });
    }

    @Override
    public List<Order> history() {
        User user = session.require(Role.CUSTOMER, Role.SELLER, Role.ADMIN);
        Integer customerId = user.getRole().equalsIgnoreCase(Role.CUSTOMER.name()) ? user.getUserId() : null;
        Integer sellerId = user.getRole().equalsIgnoreCase(Role.SELLER.name()) ? user.getUserId() : null;
        return orders.history(customerId, sellerId);
    }

    @Override
    public Order find(int id) {
        User user = session.require(Role.CUSTOMER, Role.SELLER, Role.ADMIN);
        Order order = orders.find(id).orElseThrow(() -> new AppException("Order not found."));
        authorizeRead(user, order);
        if (user.getRole().equalsIgnoreCase(Role.SELLER.name())) {
            BigDecimal sellerTotal = orders.details(id, user.getUserId()).stream()
                    .map(OrderDetail::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalAmount(sellerTotal.doubleValue());
        }
        return order;
    }

    @Override
    public List<OrderDetail> details(int id) {
        find(id);
        User user = session.currentUser();
        Integer sellerId = user.getRole().equalsIgnoreCase(Role.SELLER.name()) ? user.getUserId() : null;
        return orders.details(id, sellerId);
    }

    @Override
    public void cancel(int id) {
        User user = session.require(Role.CUSTOMER, Role.ADMIN);
        jdbc.transaction(connection -> {
            Order order = orders.lock(connection, id).orElseThrow(() -> new AppException("Order not found."));
            if (user.getRole().equalsIgnoreCase(Role.CUSTOMER.name()) && order.getCustomerId() != user.getUserId())
                throw new AppException("You can only cancel your own orders.");
            OrderStatus status = OrderStatus.valueOf(order.getOrderStatus());
            if (status != OrderStatus.PENDING && status != OrderStatus.PAID)
                throw new AppException("Only pending or paid orders can be cancelled.");
            for (OrderDetail detail : orders.details(connection, id, null)) {
                products.lock(connection, detail.productId())
                        .orElseThrow(() -> new AppException("An ordered product no longer exists."));
                products.restock(connection, detail.productId(), detail.quantity());
            }
            if (status == OrderStatus.PAID) payments.refund(connection, id);
            orders.status(connection, id, OrderStatus.CANCELLED);
            return null;
        });
    }

    @Override
    public void advance(int id, OrderStatus next) {
        session.require(Role.ADMIN);
        jdbc.transaction(connection -> {
            Order order = orders.lock(connection, id).orElseThrow(() -> new AppException("Order not found."));
            OrderStatus current = OrderStatus.valueOf(order.getOrderStatus());
            boolean allowed = (current == OrderStatus.PAID && next == OrderStatus.SHIPPED)
                    || (current == OrderStatus.SHIPPED && next == OrderStatus.DELIVERED);
            if (!allowed) throw new AppException("Allowed transitions are PAID -> SHIPPED -> DELIVERED.");
            orders.status(connection, id, next);
            return null;
        });
    }

    private void authorizeRead(User user, Order order) {
        if (user.getRole().equalsIgnoreCase(Role.CUSTOMER.name()) && user.getUserId() != order.getCustomerId())
            throw new AppException("You can only access your own orders.");
        if (user.getRole().equalsIgnoreCase(Role.SELLER.name())
                && orders.details(order.getOrderId(), user.getUserId()).isEmpty())
            throw new AppException("This order has none of your products.");
    }
}
