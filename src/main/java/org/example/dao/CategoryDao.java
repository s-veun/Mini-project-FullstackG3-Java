package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.exception.AppException;
import org.example.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDao {
    public boolean save(Category category) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, category);
        } catch (SQLException e) {
            throw new AppException("Unable to create category.", e);
        }
    }

    public boolean save(Connection connection, Category category) {
        String sql = "INSERT INTO categories (category_name, description) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getCategoryName().trim());
            statement.setString(2, category.getDescription());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to create category.", e);
        }
    }

    public List<Category> findAll() {
        String sql = "SELECT category_id, category_name, description FROM categories ORDER BY category_id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (rs.next()) categories.add(map(rs));
            return categories;
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve categories.", e);
        }
    }

    public Optional<Category> findById(int id) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findById(connection, id);
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve category.", e);
        }
    }

    public Optional<Category> findById(Connection connection, int id) {
        String sql = "SELECT category_id, category_name, description FROM categories WHERE category_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve category.", e);
        }
    }

    public boolean update(Category category) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return update(connection, category);
        } catch (SQLException e) {
            throw new AppException("Unable to update category.", e);
        }
    }

    public boolean update(Connection connection, Category category) {
        String sql = "UPDATE categories SET category_name = ?, description = ? WHERE category_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getCategoryName().trim());
            statement.setString(2, category.getDescription());
            statement.setInt(3, category.getCategoryId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to update category.", e);
        }
    }

    public boolean delete(int id) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return delete(connection, id);
        } catch (SQLException e) {
            throw new AppException("Unable to delete category. Products may still reference it.", e);
        }
    }

    public boolean delete(Connection connection, int id) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM categories WHERE category_id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to delete category. Products may still reference it.", e);
        }
    }

    public boolean existsById(int id) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return existsById(connection, id);
        } catch (SQLException e) {
            throw new AppException("Unable to check category.", e);
        }
    }

    public boolean existsById(Connection connection, int id) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM categories WHERE category_id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new AppException("Unable to check category.", e);
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        return Category.builder()
                .categoryId(rs.getInt("category_id"))
                .categoryName(rs.getString("category_name"))
                .description(rs.getString("description"))
                .build();
    }
}
