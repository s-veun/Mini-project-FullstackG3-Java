package org.example.dao;

import org.example.enums.PaymentStatus;
import org.example.exception.AppException;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class PaymentDao {
    public record PaymentRecord(int paymentId, int orderId, PaymentStatus status, BigDecimal amount, Timestamp date) {
    }

    public void record(Connection connection, int orderId, BigDecimal amount, PaymentStatus status) {
        String sql = "INSERT INTO payments (order_id, payment_status, amount, payment_date) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.setString(2, status.name());
            statement.setBigDecimal(3, amount);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Unable to record payment.", e);
        }
    }

    public void refund(Connection connection, int orderId) {
        String sql = "UPDATE payments SET payment_status = 'REFUNDED' WHERE order_id = ? AND payment_status = 'SUCCESS'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Unable to refund payment.", e);
        }
    }

    public Optional<PaymentStatus> latestStatus(Connection connection, int orderId) {
        String sql = "SELECT payment_status FROM payments WHERE order_id = ? ORDER BY payment_date DESC, payment_id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(PaymentStatus.valueOf(rs.getString(1))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve payment status.", e);
        }
    }

    public java.util.List<PaymentRecord> historyForCustomer(int customerId) {
        String sql = "SELECT p.payment_id, p.order_id, p.payment_status, p.amount, p.payment_date " +
                "FROM payments p JOIN orders o ON o.order_id = p.order_id " +
                "WHERE o.customer_id = ? ORDER BY p.payment_date DESC, p.payment_id DESC";
        try (Connection connection = org.example.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            try (ResultSet rs = statement.executeQuery()) {
                java.util.List<PaymentRecord> payments = new java.util.ArrayList<>();
                while (rs.next()) payments.add(new PaymentRecord(rs.getInt("payment_id"), rs.getInt("order_id"),
                        PaymentStatus.valueOf(rs.getString("payment_status")), rs.getBigDecimal("amount"),
                        rs.getTimestamp("payment_date")));
                return payments;
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve payment history.", e);
        }
    }
}
