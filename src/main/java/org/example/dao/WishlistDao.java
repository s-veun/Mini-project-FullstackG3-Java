package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.exception.AppException;
import org.example.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WishlistDao {
    public List<Product> find(int customerId) {
        String sql = "SELECT p.product_id, p.seller_id, p.category_id, p.product_name, p.description, p.price, " +
                "p.stock_quantity, p.active, p.created_at FROM wishlist w JOIN products p ON p.product_id = w.product_id " +
                "WHERE w.customer_id = ? ORDER BY p.product_name";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            try (ResultSet rs = statement.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (rs.next()) products.add(Product.builder().productId(rs.getInt("product_id"))
                        .sellerId(rs.getInt("seller_id")).categoryId(rs.getInt("category_id"))
                        .productName(rs.getString("product_name")).description(rs.getString("description"))
                        .price(rs.getBigDecimal("price").doubleValue()).stockQuantity(rs.getInt("stock_quantity"))
                        .active(rs.getBoolean("active")).createdAt(rs.getTimestamp("created_at")).build());
                return products;
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve wishlist.", e);
        }
    }

    public void add(int customerId, int productId) {
        String sql = "INSERT INTO wishlist (customer_id, product_id) VALUES (?, ?) " +
                "ON CONFLICT (customer_id, product_id) DO NOTHING";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            statement.setInt(2, productId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Unable to add product to wishlist.", e);
        }
    }

    public boolean remove(int customerId, int productId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM wishlist WHERE customer_id = ? AND product_id = ?")) {
            statement.setInt(1, customerId);
            statement.setInt(2, productId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AppException("Unable to remove product from wishlist.", e);
        }
    }
}
