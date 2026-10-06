package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.exception.AppException;
import org.example.model.User;

import java.sql.*;
import java.util.Optional;

public class UserDao {
    private static final String COLUMNS = "user_id, username, email, password, role, created_at";

    public boolean save(User user) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, user);
        } catch (SQLException e) {
            throw new AppException("Unable to register user. Username or email may already be in use.", e);
        }
    }

    public boolean save(Connection connection, User user) {
        String sql = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername().trim());
            statement.setString(2, user.getEmail().trim().toLowerCase());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getRole().toUpperCase());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to register user. Username or email may already be in use.", e);
        }
    }

    public Optional<User> findByUsername(String username) {
        return select("SELECT " + COLUMNS + " FROM users WHERE LOWER(username) = LOWER(?)", username);
    }

    public Optional<User> findByUsername(Connection connection, String username) {
        return select(connection, "SELECT " + COLUMNS + " FROM users WHERE LOWER(username) = LOWER(?)", username);
    }

    public Optional<User> findByEmail(String email) {
        return select("SELECT " + COLUMNS + " FROM users WHERE LOWER(email) = LOWER(?)", email);
    }

    public Optional<User> findById(int id) {
        return select("SELECT " + COLUMNS + " FROM users WHERE user_id = ?", id);
    }

    public Optional<User> findById(Connection connection, int id) {
        return select(connection, "SELECT " + COLUMNS + " FROM users WHERE user_id = ?", id);
    }

    public boolean updatePassword(int userId, String expectedOldPassword, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE user_id = ? AND password = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setInt(2, userId);
            statement.setString(3, expectedOldPassword);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to upgrade stored password.", e);
        }
    }

    public boolean updateProfile(int userId, String email, String passwordHash) {
        String sql = "UPDATE users SET email = ?, password = ? WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim().toLowerCase());
            statement.setString(2, passwordHash);
            statement.setInt(3, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new AppException("Unable to update profile. The email may already be in use.", e);
        }
    }

    public static void lock(Connection connection, int userId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT user_id FROM users WHERE user_id = ? FOR UPDATE")) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new AppException("User account not found.");
            }
        } catch (SQLException e) {
            throw new AppException("Unable to lock user account.", e);
        }
    }

    private Optional<User> select(String sql, Object parameter) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return select(connection, sql, parameter);
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve user.", e);
        }
    }

    private Optional<User> select(Connection connection, String sql, Object parameter) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter instanceof Integer id) statement.setInt(1, id);
            else statement.setString(1, (String) parameter);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AppException("Unable to retrieve user.", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return User.builder()
                .userId(rs.getInt("user_id"))
                .username(rs.getString("username"))
                .email(rs.getString("email"))
                .password(rs.getString("password"))
                .role(rs.getString("role"))
                .createdAt(rs.getTimestamp("created_at"))
                .build();
    }
}
