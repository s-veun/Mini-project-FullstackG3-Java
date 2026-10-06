package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.exception.AppException;
import org.example.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDao {
    public boolean add(int productId, int customerId, int rating, String comment) {
        String sql = "INSERT INTO reviews (product_id, customer_id, rating, comment) " +
                "SELECT ?, ?, ?, ? WHERE EXISTS (SELECT 1 FROM order_details d JOIN orders o ON o.order_id = d.order_id " +
                "WHERE d.product_id = ? AND o.customer_id = ? AND o.order_status IN ('PAID', 'SHIPPED', 'DELIVERED')) " +
                "ON CONFLICT (customer_id, product_id) DO NOTHING";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            statement.setInt(2, customerId);
            statement.setInt(3, rating);
            statement.setString(4, comment);
            statement.setInt(5, productId);
            statement.setInt(6, customerId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to save review.", e);
        }
    }

    public List<Review> findByProduct(int productId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT review_id, product_id, customer_id, rating, comment, created_at " +
                             "FROM reviews WHERE product_id = ? ORDER BY created_at DESC, review_id DESC")) {
            statement.setInt(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (rs.next()) reviews.add(new Review(rs.getInt("review_id"), rs.getInt("product_id"),
                        rs.getInt("customer_id"), rs.getInt("rating"), rs.getString("comment"),
                        rs.getTimestamp("created_at")));
                return reviews;
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve reviews.", e);
        }
    }
}
