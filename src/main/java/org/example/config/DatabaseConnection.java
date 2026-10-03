package org.example.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static String USER = "postgres";
    private static String PASSWORD = "Saveun2032";

    public static Connection getConnection() throws SQLException{
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw  new SQLException("Driver not found",e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

