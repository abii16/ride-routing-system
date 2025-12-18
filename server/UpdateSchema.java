package server;

import java.sql.*;

public class UpdateSchema {
  static final String DB_URL = "jdbc:mysql://localhost:3306/ride_sharing_system";
  static final String USER = "root";
  static final String PASS = "";

  public static void main(String[] args) {
    try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        Statement stmt = conn.createStatement()) {

      System.out.println("Updating Database Schema...");

      // Add location columns to passengers if they don't exist
      try {
        stmt.executeUpdate("ALTER TABLE passengers ADD COLUMN latitude DOUBLE DEFAULT NULL");
        stmt.executeUpdate("ALTER TABLE passengers ADD COLUMN longitude DOUBLE DEFAULT NULL");
        System.out.println("Added location columns to 'passengers' table.");
      } catch (SQLException e) {
        System.out.println("Columns might already exist: " + e.getMessage());
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
