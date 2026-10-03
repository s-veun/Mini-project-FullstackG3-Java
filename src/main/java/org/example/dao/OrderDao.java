package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.model.CartItem;
import java.sql.*;
import java.util.List;

public class OrderDao {
    public boolean createOrderWithTransaction(int customerId, double totalAmount, List<CartItem> cartItems) {
        String insertOrder = "INSERT INTO orders (customer_id, total_amount, order_status) VALUES (?, ?, 'PENDING') RETURNING order_id";
        String insertDetail = "INSERT INTO order_details (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, (SELECT price FROM products WHERE product_id = ?))";
        String updateStock = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";
        String clearCart = "DELETE FROM shopping_cart WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // ចាប់ផ្តើម Transaction

            int orderId;
            try (PreparedStatement pstmt = conn.prepareStatement(insertOrder)) {
                pstmt.setInt(1, customerId);
                pstmt.setDouble(2, totalAmount);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            for (CartItem item : cartItems) {
                try (PreparedStatement detailStmt = conn.prepareStatement(insertDetail);
                     PreparedStatement stockStmt = conn.prepareStatement(updateStock)) {

                    detailStmt.setInt(1, orderId);
                    detailStmt.setInt(2, item.getProductId());
                    detailStmt.setInt(3, item.getQuantity());
                    detailStmt.setInt(4, item.getProductId());
                    detailStmt.executeUpdate();

                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, item.getProductId());
                    stockStmt.executeUpdate();
                }
            }

            try (PreparedStatement cartStmt = conn.prepareStatement(clearCart)) {
                cartStmt.setInt(1, customerId);
                cartStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}