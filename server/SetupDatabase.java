package server;

import java.sql.*;

public class SetupDatabase {
  static final String DB_URL = "jdbc:mysql://localhost:3306/";
  static final String USER = "root";
  static final String PASS = "";

  public static void main(String[] args) {
    try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        Statement stmt = conn.createStatement()) {

      System.out.println("Connected to MySQL server...");

      // Create Database
      String sql = "CREATE DATABASE IF NOT EXISTS ride_sharing_system";
      stmt.executeUpdate(sql);
      System.out.println("Database 'ride_sharing_system' created successfully.");

      // Select Database
      stmt.executeUpdate("USE ride_sharing_system");

      // Create Passengers Table
      String passengers = "CREATE TABLE IF NOT EXISTS passengers (" +
          "id INT AUTO_INCREMENT PRIMARY KEY, " +
          "username VARCHAR(50) NOT NULL UNIQUE, " +
          "password VARCHAR(255) NOT NULL, " +
          "phone VARCHAR(20), " +
          "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
      stmt.executeUpdate(passengers);
      System.out.println("Table 'passengers' created.");

      // Create Drivers Table
      String drivers = "CREATE TABLE IF NOT EXISTS drivers (" +
          "id INT AUTO_INCREMENT PRIMARY KEY, " +
          "username VARCHAR(50) NOT NULL UNIQUE, " +
          "password VARCHAR(255) NOT NULL, " +
          "phone VARCHAR(20), " +
          "is_available BOOLEAN DEFAULT TRUE, " +
          "latitude DOUBLE, " +
          "longitude DOUBLE, " +
          "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
      stmt.executeUpdate(drivers);
      System.out.println("Table 'drivers' created.");

      // Create Rides Table
      String rides = "CREATE TABLE IF NOT EXISTS rides (" +
          "id INT AUTO_INCREMENT PRIMARY KEY, " +
          "passenger_id INT, " +
          "driver_id INT, " +
          "start_latitude DOUBLE, " +
          "start_longitude DOUBLE, " +
          "end_latitude DOUBLE, " +
          "end_longitude DOUBLE, " +
          "status ENUM('REQUESTED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'REQUESTED', " +
          "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
          "FOREIGN KEY (passenger_id) REFERENCES passengers(id), " +
          "FOREIGN KEY (driver_id) REFERENCES drivers(id))";
      stmt.executeUpdate(rides);
      System.out.println("Table 'rides' created.");

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
