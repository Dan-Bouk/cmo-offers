package com.cmo.offers.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
	
	private static String getRequiredEnv(String name) {
	    String value = System.getenv(name);
	    if (value == null || value.isBlank()) {
	        throw new IllegalStateException(
	            "Missing environment variable: " + name
	        );
	    }
	    return value;
	}

	private static final String URL = getRequiredEnv("DB_URL");
	private static final String USER = getRequiredEnv("DB_USER");
	private static final String PASSWORD = getRequiredEnv("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    // --- TEST METHOD ---
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Connected to PostgreSQL successfully!");
                System.out.println("AutoCommit: " + conn.getAutoCommit());
            }
        } catch (SQLException e) {
            System.err.println("❌ Connection failed.");
            e.printStackTrace();
        }
    }
}