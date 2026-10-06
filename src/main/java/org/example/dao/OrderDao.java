package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.enums.OrderStatus;
import org.example.exception.AppException;
import org.example.model.Order;
import org.example.model.OrderDetail;
import org.example.model.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDao {
    public int create(Connection connection, int customerId, BigDecimal total) {
        String sql = "INSERT INTO orders (customer_id, total_amount, order_status) VALUES (?, ?, 'PENDING') RETURNING order_id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            statement.setBigDecimal(2, total);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new AppException("Unable to create order.");
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new AppException("Unable to create order.", e);
        }
    }

    public void addDetail(Connection connection, int orderId, Product product, int quantity) {
        String sql = "INSERT INTO order_details (order_id, product_id, seller_id, product_name, quantity, unit_price) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.setInt(2, product.getProductId());
            statement.setInt(3, product.getSellerId());
            statement.setString(4, product.getProductName());
            statement.setInt(5, quantity);
            statement.setBigDecimal(6, BigDecimal.valueOf(product.getPrice()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Unable to save order item.", e);
        }
    }

    public List<Order> history(Integer customerId, Integer sellerId) {
        String sql;
        if (sellerId != null) {
            sql = "SELECT o.order_id, o.customer_id, SUM(d.unit_price * d.quantity) AS total_amount, " +
                    "o.order_status, o.order_date FROM orders o JOIN order_details d ON d.order_id = o.order_id " +
                    "WHERE d.seller_id = ? GROUP BY o.order_id, o.customer_id, o.order_status, o.order_date " +
                    "ORDER BY o.order_date DESC";
        } else if (customerId != null) {
            sql = "SELECT order_id, customer_id, total_amount, order_status, order_date FROM orders " +
                    "WHERE customer_id = ? ORDER BY order_date DESC";
        } else {
            sql = "SELECT order_id, customer_id, total_amount, order_status, order_date FROM orders ORDER BY order_date DESC";
        }
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (sellerId != null) statement.setInt(1, sellerId);
            else if (customerId != null) statement.setInt(1, customerId);
            try (ResultSet rs = statement.executeQuery()) {
                List<Order> result = new ArrayList<>();
                while (rs.next()) result.add(mapOrder(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve order history.", e);
        }
    }

    public Optional<Order> find(int id) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return selectOrder(connection, "SELECT order_id, customer_id, total_amount, order_status, order_date " +
                    "FROM orders WHERE order_id = ?", id);
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve order.", e);
        }
    }

    public Optional<Order> lock(Connection connection, int id) {
        return selectOrder(connection, "SELECT order_id, customer_id, total_amount, order_status, order_date " +
                "FROM orders WHERE order_id = ? FOR UPDATE", id);
    }

    public List<OrderDetail> details(int orderId, Integer sellerId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return details(connection, orderId, sellerId);
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve order items.", e);
        }
    }

    public List<OrderDetail> details(Connection connection, int orderId, Integer sellerId) {
        String sql = "SELECT order_details_id, order_id, product_id, seller_id, product_name, quantity, unit_price " +
                "FROM order_details WHERE order_id = ? AND (? IS NULL OR seller_id = ?) ORDER BY order_details_id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            nullableInt(statement, 2, sellerId);
            nullableInt(statement, 3, sellerId);
            try (ResultSet rs = statement.executeQuery()) {
                List<OrderDetail> result = new ArrayList<>();
                while (rs.next()) {
                    BigDecimal price = rs.getBigDecimal("unit_price");
                    result.add(new OrderDetail(rs.getInt("order_details_id"), rs.getInt("order_id"),
                            rs.getInt("product_id"), rs.getString("product_name"), price,
                            rs.getInt("quantity"), price.multiply(BigDecimal.valueOf(rs.getInt("quantity"))),
                            rs.getInt("seller_id")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve order items.", e);
        }
    }

    public void status(Connection connection, int orderId, OrderStatus status) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE orders SET order_status = ? WHERE order_id = ?")) {
            statement.setString(1, status.name());
            statement.setInt(2, orderId);
            if (statement.executeUpdate() != 1) throw new AppException("Unable to update order status.");
        } catch (SQLException e) {
            throw new AppException("Unable to update order status.", e);
        }
    }

    private Optional<Order> selectOrder(Connection connection, String sql, int id) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapOrder(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve order.", e);
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        return Order.builder()
                .orderId(rs.getInt("order_id"))
                .customerId(rs.getInt("customer_id"))
                .totalAmount(rs.getBigDecimal("total_amount").doubleValue())
                .orderStatus(rs.getString("order_status"))
                .orderDate(rs.getTimestamp("order_date"))
                .build();
    }

    private void nullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) statement.setNull(index, Types.INTEGER);
        else statement.setInt(index, value);
    }
}
