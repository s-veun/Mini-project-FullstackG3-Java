package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.exception.AppException;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDao {
    public record ProductPerformance(int productId, String productName, int unitsSold, BigDecimal revenue) {
    }

    public record SalesRow(int orderId, Timestamp orderDate, String productName, String sellerName,
                           int quantity, BigDecimal unitPrice, BigDecimal revenue) {
    }

    public List<SalesRow> sales(Integer sellerId) {
        String sql = "SELECT o.order_id, o.order_date, d.product_name, u.username AS seller_name, d.quantity, " +
                "d.unit_price, d.unit_price * d.quantity AS revenue " +
                "FROM orders o JOIN order_details d ON d.order_id = o.order_id " +
                "JOIN users u ON u.user_id = d.seller_id " +
                "WHERE o.order_status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
                (sellerId == null ? "" : "AND d.seller_id = ? ") +
                "ORDER BY o.order_date DESC, o.order_id, d.order_details_id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (sellerId != null) statement.setInt(1, sellerId);
            try (ResultSet rs = statement.executeQuery()) {
                List<SalesRow> rows = new ArrayList<>();
                while (rs.next()) rows.add(new SalesRow(rs.getInt("order_id"), rs.getTimestamp("order_date"),
                        rs.getString("product_name"), rs.getString("seller_name"), rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"), rs.getBigDecimal("revenue")));
                return rows;
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve sales report.", e);
        }
    }

    public BigDecimal totalRevenue(Integer sellerId) {
        String sql = "SELECT COALESCE(SUM(d.unit_price * d.quantity), 0) AS total_revenue " +
                "FROM orders o JOIN order_details d ON d.order_id = o.order_id " +
                "WHERE o.order_status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
                (sellerId == null ? "" : "AND d.seller_id = ?");
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (sellerId != null) statement.setInt(1, sellerId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("total_revenue");
            }
        } catch (SQLException e) {
            throw new AppException("Unable to calculate total revenue.", e);
        }
    }

    public List<ProductPerformance> topProducts(Integer sellerId, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Report limit must be between 1 and 100.");
        String sql = "SELECT d.product_id, d.product_name, SUM(d.quantity) AS units_sold, " +
                "SUM(d.unit_price * d.quantity) AS revenue FROM orders o " +
                "JOIN order_details d ON d.order_id = o.order_id " +
                "WHERE o.order_status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
                (sellerId == null ? "" : "AND d.seller_id = ? ") +
                "GROUP BY d.product_id, d.product_name ORDER BY units_sold DESC, revenue DESC LIMIT ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (sellerId != null) statement.setInt(index++, sellerId);
            statement.setInt(index, limit);
            try (ResultSet rs = statement.executeQuery()) {
                List<ProductPerformance> result = new ArrayList<>();
                while (rs.next()) result.add(new ProductPerformance(rs.getInt("product_id"),
                        rs.getString("product_name"), rs.getInt("units_sold"), rs.getBigDecimal("revenue")));
                return result;
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve top-selling products.", e);
        }
    }
}
