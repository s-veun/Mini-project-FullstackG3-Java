package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.model.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDao {

    public boolean save(Category category) {
        String sql = "INSERT INTO categories (category_name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getCategoryName());
            pstmt.setString(2, category.getDescription());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Category> findAll() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY category_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(Category.builder()
                        .categoryId(rs.getInt("category_id"))
                        .categoryName(rs.getString("category_name"))
                        .description(rs.getString("description"))
                        .build());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public Optional<Category> findById(int categoryId) {
        String sql = "SELECT * FROM categories WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(Category.builder()
                            .categoryId(rs.getInt("category_id"))
                            .categoryName(rs.getString("category_name"))
                            .description(rs.getString("description"))
                            .build());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean update(Category category) {
        String sql = "UPDATE categories SET category_name = ?, description = ? WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getCategoryName());
            pstmt.setString(2, category.getDescription());
            pstmt.setInt(3, category.getCategoryId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existsById(int categoryId) {
        String sql = "SELECT 1 FROM categories WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}