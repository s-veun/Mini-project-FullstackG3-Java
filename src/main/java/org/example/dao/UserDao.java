package org.example.dao;

import org.example.config.DatabaseConnection;
import org.example.model.User;

import java.sql.*;
import java.util.Optional;

public class UserDao {
    public boolean save(User user) {
        String sql = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getRole());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(User.builder()
                            .userId(rs.getInt("user_id"))
                            .username(rs.getString("username"))
                            .password(rs.getString("password"))
                            .email(rs.getString("email"))
                            .role(rs.getString("role"))
                            .createdAt(rs.getTimestamp("created_at"))
                            .build());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}