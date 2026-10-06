package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.exception.AppException;
import org.example.model.Product;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDao {
    private static final String COLUMNS = "product_id, seller_id, category_id, product_name, description, price, " +
            "stock_quantity, active, created_at";

    public boolean save(Product product) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, product);
        } catch (SQLException e) {
            throw new AppException("Unable to save product.", e);
        }
    }

    public boolean save(Connection connection, Product product) {
        String sql = "INSERT INTO products (seller_id, category_id, product_name, description, price, stock_quantity, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, product.getSellerId());
            statement.setInt(2, product.getCategoryId());
            statement.setString(3, product.getProductName());
            statement.setString(4, product.getDescription());
            statement.setBigDecimal(5, java.math.BigDecimal.valueOf(product.getPrice()));
            statement.setInt(6, product.getStockQuantity());
            statement.setBoolean(7, product.isActive());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to save product.", e);
        }
    }

    public List<Product> findAll() {
        return queryProducts("SELECT " + COLUMNS + " FROM products ORDER BY product_id", null);
    }

    public List<Product> findActive() {
        return queryProducts("SELECT " + COLUMNS + " FROM products WHERE active = TRUE ORDER BY product_id", null);
    }

    public List<Product> search(String term, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS)
                .append(" FROM products WHERE active = TRUE ");
        List<Object> parameters = new ArrayList<>();
        if (term != null && !term.isBlank()) {
            sql.append("AND (product_name ILIKE ? OR COALESCE(description, '') ILIKE ?) ");
            String escaped = term.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            parameters.add("%" + escaped + "%");
            parameters.add("%" + escaped + "%");
        }
        if (categoryId != null) {
            sql.append("AND category_id = ? ");
            parameters.add(categoryId);
        }
        if (minPrice != null) {
            sql.append("AND price >= ? ");
            parameters.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append("AND price <= ? ");
            parameters.add(maxPrice);
        }
        sql.append("ORDER BY product_name, product_id");
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                Object value = parameters.get(i);
                if (value instanceof Integer integer) statement.setInt(i + 1, integer);
                else if (value instanceof BigDecimal decimal) statement.setBigDecimal(i + 1, decimal);
                else statement.setString(i + 1, (String) value);
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<Product> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new AppException("Unable to search products.", e);
        }
    }

    public List<Product> findBySeller(int sellerId) {
        return queryProducts("SELECT " + COLUMNS + " FROM products WHERE seller_id = ? ORDER BY product_id", sellerId);
    }

    public Optional<Product> findById(int id) {
        return findById(null, id);
    }

    public Optional<Product> findById(Connection connection, int id) {
        String sql = "SELECT " + COLUMNS + " FROM products WHERE product_id = ?";
        if (connection != null) {
            return selectOne(connection, sql, id);
        }
        try (Connection c = DatabaseConnection.getConnection()) {
            return selectOne(c, sql, id);
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve product.", e);
        }
    }

    public Optional<Product> lock(Connection connection, int id) {
        return selectOne(connection, "SELECT " + COLUMNS + " FROM products WHERE product_id = ? FOR UPDATE", id);
    }

    public boolean update(Product product) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return update(connection, product);
        } catch (SQLException e) {
            throw new AppException("Unable to update product.", e);
        }
    }

    public boolean update(Connection connection, Product product) {
        String sql = "UPDATE products SET category_id = ?, product_name = ?, description = ?, price = ?, " +
                "stock_quantity = ?, active = ? WHERE product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEditable(statement, product);
            statement.setInt(7, product.getProductId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to update product.", e);
        }
    }

    public boolean setActive(int productId, boolean active) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return setActive(connection, productId, active);
        } catch (SQLException e) {
            throw new AppException("Unable to update product availability.", e);
        }
    }

    public boolean setActive(Connection connection, int productId, boolean active) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET active = ? WHERE product_id = ?")) {
            statement.setBoolean(1, active);
            statement.setInt(2, productId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to update product availability.", e);
        }
    }

    public void reserve(Connection connection, int productId, int quantity) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET stock_quantity = stock_quantity - ? " +
                        "WHERE product_id = ? AND active = TRUE AND stock_quantity >= ?")) {
            statement.setInt(1, quantity);
            statement.setInt(2, productId);
            statement.setInt(3, quantity);
            if (statement.executeUpdate() != 1) throw new AppException("Product stock changed during checkout.");
        } catch (SQLException e) {
            throw new AppException("Unable to reserve product stock.", e);
        }
    }

    public void restock(Connection connection, int productId, int quantity) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET stock_quantity = stock_quantity + ? WHERE product_id = ?")) {
            statement.setInt(1, quantity);
            statement.setInt(2, productId);
            if (statement.executeUpdate() != 1) throw new AppException("Unable to restore product stock.");
        } catch (SQLException e) {
            throw new AppException("Unable to restore product stock.", e);
        }
    }

    private List<Product> queryProducts(String sql, Integer parameter) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) statement.setInt(1, parameter);
            try (ResultSet rs = statement.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (rs.next()) products.add(map(rs));
                return products;
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve products.", e);
        }
    }

    private Optional<Product> selectOne(Connection connection, String sql, int id) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve product.", e);
        }
    }

    private Product map(ResultSet rs) throws SQLException {
        return Product.builder()
                .productId(rs.getInt("product_id"))
                .sellerId(rs.getInt("seller_id"))
                .categoryId(rs.getInt("category_id"))
                .productName(rs.getString("product_name"))
                .description(rs.getString("description"))
                .price(rs.getBigDecimal("price").doubleValue())
                .stockQuantity(rs.getInt("stock_quantity"))
                .active(rs.getBoolean("active"))
                .createdAt(rs.getTimestamp("created_at"))
                .build();
    }

    private void bindEditable(PreparedStatement statement, Product product) throws SQLException {
        statement.setInt(1, product.getCategoryId());
        statement.setString(2, product.getProductName());
        statement.setString(3, product.getDescription());
        statement.setBigDecimal(4, java.math.BigDecimal.valueOf(product.getPrice()));
        statement.setInt(5, product.getStockQuantity());
        statement.setBoolean(6, product.isActive());
    }
}
