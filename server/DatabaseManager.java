package server;

import java.sql.*;

public class DatabaseManager {
  private static final String URL = "jdbc:mysql://localhost:3306/ride_sharing_system";
  private static final String USER = "root"; // Default XAMPP user
  private static final String PASSWORD = ""; // Default XAMPP password (empty)

  private Connection connection;

  public DatabaseManager() {
    try {
      // Load the driver class
      Class.forName("com.mysql.cj.jdbc.Driver");
      this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
      System.out.println("Database connected successfully.");
    } catch (ClassNotFoundException e) {
      System.err.println("MySQL JDBC Driver not found. details: " + e.getMessage());
    } catch (SQLException e) {
      System.err.println("Connection failed. details: " + e.getMessage());
    }
  }

  public Connection getConnection() {
    return connection;
  }

  // Find nearest available driver
  // Returns Driver Username or null if none
  public String findNearestDriver(double pLat, double pLon) {
    String query = "SELECT username, latitude, longitude, " +
        "(6371 * acos(cos(radians(?)) * cos(radians(latitude)) * cos(radians(longitude) - radians(?)) + sin(radians(?)) * sin(radians(latitude)))) AS distance "
        +
        "FROM drivers WHERE is_available = TRUE HAVING distance < 50 ORDER BY distance ASC LIMIT 1";

    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
      pstmt.setDouble(1, pLat);
      pstmt.setDouble(2, pLon);
      pstmt.setDouble(3, pLat);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        return rs.getString("username");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  // Create a new Ride
  public void createRide(String passenger, String driver, double lat, double lon) {
    // 1. Get IDs (Simplification: assuming usernames are unique and we could
    // sub-query, but lets keep it simple)
    // 2. Insert Ride
    // 3. Mark Driver Busy
    try {
      // Mark Driver Busy
      String updateDriver = "UPDATE drivers SET is_available = FALSE WHERE username = ?";
      try (PreparedStatement p1 = connection.prepareStatement(updateDriver)) {
        p1.setString(1, driver);
        p1.executeUpdate();
      }

      // Create Ride Record
      // Note: In a real system we would look up IDs first. For now, we will trust the
      // flow.
      // Insert with IDs by looking them up (subquery)
      String insertRide = "INSERT INTO rides (passenger_id, driver_id, start_latitude, start_longitude, status) " +
          "VALUES ((SELECT id FROM passengers WHERE username=?), (SELECT id FROM drivers WHERE username=?), ?, ?, 'ASSIGNED')";
      try (PreparedStatement p2 = connection.prepareStatement(insertRide)) {
        p2.setString(1, passenger);
        p2.setString(2, driver);
        p2.setDouble(3, lat);
        p2.setDouble(4, lon);
        p2.executeUpdate();
      }
      System.out.println("Ride created: " + passenger + " -> " + driver);

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  // Example method: Register a new passenger
  public boolean registerPassenger(String username, String password, String phone) {
    String query = "INSERT INTO passengers (username, password, phone) VALUES (?, ?, ?)";
    try (PreparedStatement callback = connection.prepareStatement(query)) {
      callback.setString(1, username);
      callback.setString(2, password);
      callback.setString(3, phone);
      int rows = callback.executeUpdate();
      return rows > 0;
    } catch (SQLException e) {
      System.err.println("Error registering passenger: " + e.getMessage());
      return false;
    }
  }

  // Example method: Register a new driver
  public boolean registerDriver(String username, String password, String phone) {
    String query = "INSERT INTO drivers (username, password, phone) VALUES (?, ?, ?)";
    try (PreparedStatement callback = connection.prepareStatement(query)) {
      callback.setString(1, username);
      callback.setString(2, password);
      callback.setString(3, phone);
      int rows = callback.executeUpdate();
      return rows > 0;
    } catch (SQLException e) {
      System.err.println("Error registering driver: " + e.getMessage());
      return false;
    }
  }

  // Validate Login
  public boolean validateLogin(String role, String username, String password) {
    String table = role.equalsIgnoreCase("PASSENGER") ? "passengers" : "drivers";
    String query = "SELECT id FROM " + table + " WHERE username = ? AND password = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
      pstmt.setString(1, username);
      pstmt.setString(2, password);
      try (ResultSet rs = pstmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      System.err.println("Login error: " + e.getMessage());
    }
    return false;
  }

  // Update Passenger Location
  public void updatePassengerLocation(String username, double lat, double lon) {
    String query = "UPDATE passengers SET latitude = ?, longitude = ? WHERE username = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
      pstmt.setDouble(1, lat);
      pstmt.setDouble(2, lon);
      pstmt.setString(3, username);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  // Update Driver Location
  public void updateDriverLocation(String username, double lat, double lon) {
    String query = "UPDATE drivers SET latitude = ?, longitude = ? WHERE username = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
      pstmt.setDouble(1, lat);
      pstmt.setDouble(2, lon);
      pstmt.setString(3, username);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  // Export Data to JSON for the Web Map
  public void exportDataToJSON(String filePath) {
    StringBuilder json = new StringBuilder();
    json.append("{ \"passengers\": [");

    try (Statement stmt = connection.createStatement()) {
      // Get Passengers (only those with location)
      ResultSet rs = stmt
          .executeQuery("SELECT username, latitude, longitude FROM passengers WHERE latitude IS NOT NULL");
      boolean first = true;
      while (rs.next()) {
        if (!first)
          json.append(",");
        json.append(String.format("{\"username\":\"%s\", \"lat\":%f, \"lng\":%f}",
            rs.getString("username"), rs.getDouble("latitude"), rs.getDouble("longitude")));
        first = false;
      }
      json.append("], \"drivers\": [");

      // Get Drivers
      rs = stmt
          .executeQuery("SELECT username, latitude, longitude, is_available FROM drivers WHERE latitude IS NOT NULL");
      first = true;
      while (rs.next()) {
        if (!first)
          json.append(",");
        json.append(String.format("{\"username\":\"%s\", \"lat\":%f, \"lng\":%f, \"status\":\"%s\"}",
            rs.getString("username"), rs.getDouble("latitude"), rs.getDouble("longitude"),
            rs.getBoolean("is_available") ? "AVAILABLE" : "BUSY"));
        first = false;
      }
      json.append("] }");

      // Write file
      try (java.io.PrintWriter out = new java.io.PrintWriter(filePath)) {
        out.println(json.toString());
      }

    } catch (Exception e) {
      System.err.println("Error exporting map data: " + e.getMessage());
    }
  }
}
