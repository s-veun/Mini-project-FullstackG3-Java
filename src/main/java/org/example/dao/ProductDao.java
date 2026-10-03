package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    public boolean save(Product product) {
        String sql = "INSERT INTO products (seller_id, category_id, product_name, description, price, stock_quantity) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, product.getSellerId());
            pstmt.setInt(2, product.getCategoryId());
            pstmt.setString(3, product.getProductName());
            pstmt.setString(4, product.getDescription());
            pstmt.setDouble(5, product.getPrice());
            pstmt.setInt(6, product.getStockQuantity());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(Product.builder()
                        .productId(rs.getInt("product_id"))
                        .sellerId(rs.getInt("seller_id"))
                        .categoryId(rs.getInt("category_id"))
                        .productName(rs.getString("product_name"))
                        .description(rs.getString("description"))
                        .price(rs.getDouble("price"))
                        .stockQuantity(rs.getInt("stock_quantity"))
                        .createdAt(rs.getTimestamp("created_at"))
                        .build());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
}