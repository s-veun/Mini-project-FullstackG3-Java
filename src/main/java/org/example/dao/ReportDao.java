package org.example.dao;

import org.example.config.DatabaseConnection;
import java.sql.*;

public class ReportDao {
    public void printRevenueReport() {
        String sql = "SELECT SUM(total_amount) AS total_revenue, COUNT(*) AS total_orders FROM orders WHERE order_status != 'CANCELLED'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                System.out.println("=== Financial Sales Report ===");
                System.out.println("Total Orders: " + rs.getInt("total_orders"));
                System.out.println("Total Revenue: $" + rs.getDouble("total_revenue"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}