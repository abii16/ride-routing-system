package services.database;

import common.Message;
import common.MessageType;
import common.JSONUtil;

import java.sql.*;
import java.io.*;
import java.util.*;

/**
 * Database Manager handles all MySQL database operations.
 * This is used by the DatabaseServiceServer to perform actual database work.
 */
public class DatabaseManager {
    // Allow configuring the DB Host (default to localhost)
    private static String DB_HOST = "localhost";
    static {
        // Try config file first
        try (InputStream input = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            if (prop.getProperty("MYSQL_HOST") != null && !prop.getProperty("MYSQL_HOST").isEmpty()) {
                DB_HOST = prop.getProperty("MYSQL_HOST");
            }
        } catch (IOException ex) {
            /* Ignore */ }

        // Fallback to Env
        if (System.getenv("MYSQL_HOST") != null) {
            DB_HOST = System.getenv("MYSQL_HOST");
        }
    }

    private static final String URL = "jdbc:mysql://" + DB_HOST + ":3306/ride_sharing_distributed";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Connection connection;
    private static final Object dbLock = new Object();

    public DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 1. Attempt to create the database if it doesn't exist
            try (Connection setupConn = DriverManager.getConnection("jdbc:mysql://" + DB_HOST + ":3306/", USER,
                    PASSWORD);
                    Statement stmt = setupConn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS ride_sharing_distributed");
            } catch (SQLException e) {
                System.out.println(
                        "[DatabaseManager] Info: Could not auto-create database (might exist or permission denied): "
                                + e.getMessage());
            }

            // 2. Connect to the database
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DatabaseManager] Connected to MySQL database successfully.");

            // 3. Initialize Tables (Auto-Migration)
            initializeSchema();
            updateSchema(); // Ensure new columns exist

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Ensure library is in classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed. Verify MySQL is running at " + DB_HOST
                    + ":3306 and user '" + USER + "' has access.", e);
        }
    }

    private void updateSchema() {
        // Migration to add columns to existing drivers table if they don't exist
        String[] columns = {
                "ADD COLUMN full_name VARCHAR(100)",
                "ADD COLUMN dob VARCHAR(50)",
                "ADD COLUMN gender VARCHAR(20)",
                "ADD COLUMN nationality VARCHAR(50)",
                "ADD COLUMN id_number VARCHAR(50)",
                "ADD COLUMN email VARCHAR(100)",
                "ADD COLUMN address TEXT",
                "ADD COLUMN license_number VARCHAR(50)",
                "ADD COLUMN license_type VARCHAR(50)",
                "ADD COLUMN license_issue_date VARCHAR(50)",
                "ADD COLUMN license_expiry_date VARCHAR(50)",
                "ADD COLUMN vehicle_type VARCHAR(50)",
                "ADD COLUMN vehicle_model VARCHAR(50)",
                "ADD COLUMN vehicle_year INT",
                "ADD COLUMN license_plate VARCHAR(50)"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String col : columns) {
                try {
                    stmt.executeUpdate("ALTER TABLE drivers " + col);
                    System.out.println("[DatabaseManager] Added missing column: " + col);
                } catch (SQLException e) {
                    // Ignore "Duplicate column name" error (Code 1060)
                    if (e.getErrorCode() != 1060) {
                        System.err.println("[DatabaseManager] Migration warning: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeSchema() {
        try (Statement stmt = connection.createStatement()) {
            // Passengers
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS passengers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "phone VARCHAR(20), " +
                    "latitude DOUBLE DEFAULT 0.0, " +
                    "longitude DOUBLE DEFAULT 0.0, " +
                    "is_login TINYINT(1) DEFAULT 0)");

            // Drivers - Including all fields used in registerDriverDetailed
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS drivers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "phone VARCHAR(20), " +
                    "latitude DOUBLE DEFAULT 0.0, " +
                    "longitude DOUBLE DEFAULT 0.0, " +
                    "is_available TINYINT(1) DEFAULT 1, " +
                    "is_login TINYINT(1) DEFAULT 0, " +
                    "full_name VARCHAR(100), " +
                    "dob VARCHAR(50), " + // broadened
                    "gender VARCHAR(20), " +
                    "nationality VARCHAR(50), " +
                    "id_number VARCHAR(50), " +
                    "email VARCHAR(100), " +
                    "address TEXT, " +
                    "license_number VARCHAR(50), " +
                    "license_type VARCHAR(50), " +
                    "license_issue_date VARCHAR(50), " +
                    "license_expiry_date VARCHAR(50), " +
                    "vehicle_type VARCHAR(50), " +
                    "vehicle_model VARCHAR(50), " +
                    "vehicle_year INT, " +
                    "license_plate VARCHAR(50), " +
                    "status VARCHAR(20) DEFAULT 'APPROVED')");

            // Rides
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS rides (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "passenger_id INT NOT NULL, " +
                    "driver_id INT DEFAULT NULL, " +
                    "status VARCHAR(20) DEFAULT 'REQUESTED', " +
                    "start_latitude DOUBLE NOT NULL, " +
                    "start_longitude DOUBLE NOT NULL, " +
                    "dest_latitude DOUBLE NOT NULL, " +
                    "dest_longitude DOUBLE NOT NULL, " +
                    "start_address VARCHAR(255), " +
                    "dest_address VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (passenger_id) REFERENCES passengers(id))");

            // Ride History
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ride_status_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "ride_id INT NOT NULL, " +
                    "status VARCHAR(20), " +
                    "latitude DOUBLE, " +
                    "longitude DOUBLE, " +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            System.out.println("[DatabaseManager] Database schema initialized.");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Schema initialization warning: " + e.getMessage());
        }
    }

    /**
     * Register a new passenger
     */
    public synchronized Message registerPassenger(String username, String password, String phone) {
        String query = "INSERT INTO passengers (username, password, phone) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, phone);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    Message response = new Message(MessageType.DB_RESPONSE);
                    response.addPayload("success", true);
                    response.addPayload("passengerId", id);
                    response.addPayload("message", "Passenger registered successfully");
                    return response;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error registering passenger: " + e.getMessage());
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", false);
            response.addPayload("error", e.getMessage());
            return response;
        }

        Message response = new Message(MessageType.DB_RESPONSE);
        response.addPayload("success", false);
        response.addPayload("error", "Registration failed");
        return response;
    }

    /**
     * Register a new driver
     */
    public synchronized Message registerDriver(String username, String password, String phone) {
        String query = "INSERT INTO drivers (username, password, phone, status) VALUES (?, ?, ?, 'APPROVED')";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, phone);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    Message response = new Message(MessageType.DB_RESPONSE);
                    response.addPayload("success", true);
                    response.addPayload("driverId", id);
                    response.addPayload("message", "Driver registered successfully");
                    return response;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error registering driver: " + e.getMessage());
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", false);
            response.addPayload("error", e.getMessage());
            return response;
        }

        Message response = new Message(MessageType.DB_RESPONSE);
        response.addPayload("success", false);
        response.addPayload("error", "Registration failed");
        return response;
    }

    public synchronized Message registerDriverDetailed(Map<String, Object> data) {
        String query = "INSERT INTO drivers (username, password, phone, full_name, dob, gender, nationality, id_number, email, address, "
                +
                "license_number, license_type, license_issue_date, license_expiry_date, vehicle_type, vehicle_model, vehicle_year, license_plate, status) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            // Helper to safely get string
            pstmt.setString(1, getStr(data, "username"));
            pstmt.setString(2, getStr(data, "password"));
            pstmt.setString(3, getStr(data, "phone"));
            pstmt.setString(4, getStr(data, "full_name"));
            pstmt.setString(5, getStr(data, "dob"));
            pstmt.setString(6, getStr(data, "gender"));
            pstmt.setString(7, getStr(data, "nationality"));
            pstmt.setString(8, getStr(data, "id_number"));
            pstmt.setString(9, getStr(data, "email"));
            pstmt.setString(10, getStr(data, "address"));
            pstmt.setString(11, getStr(data, "license_number"));
            pstmt.setString(12, getStr(data, "license_type"));
            pstmt.setString(13, getStr(data, "license_issue_date"));
            pstmt.setString(14, getStr(data, "license_expiry_date"));
            pstmt.setString(15, getStr(data, "vehicle_type"));
            pstmt.setString(16, getStr(data, "vehicle_model"));

            // Safe Integer parsing
            int year = 2000;
            try {
                Object yObj = data.get("vehicle_year");
                if (yObj != null)
                    year = Integer.parseInt(yObj.toString());
            } catch (Exception e) {
            }
            pstmt.setInt(17, year);

            pstmt.setString(18, getStr(data, "license_plate"));

            pstmt.executeUpdate();
            System.out.println(
                    "[DatabaseManager] Driver " + getStr(data, "username") + " submitted application successfully.");

            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", true);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[DatabaseManager] registerDriverDetailed FAILED: " + e.getMessage());
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", false);
            response.addPayload("error", e.getMessage() != null ? e.getMessage() : "Database Error (See Server Logs)");
            return response;
        }
    }

    private String getStr(Map<String, Object> data, String key) {
        Object val = data.get(key);
        return val != null ? val.toString() : "";
    }

    public synchronized Message getPendingDrivers() {
        String query = "SELECT * FROM drivers WHERE status = 'PENDING'";
        List<Map<String, Object>> pending = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= count; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                pending.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Message response = new Message(MessageType.DB_RESPONSE);
        response.addPayload("success", true);
        response.addPayload("drivers", JSONUtil.toJSON(pending));
        return response;
    }

    public synchronized Message approveDriver(String username, boolean approve) {
        String status = approve ? "APPROVED" : "REJECTED";
        String query = "UPDATE drivers SET status = ?, is_available = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setBoolean(2, approve);
            pstmt.setString(3, username);
            pstmt.executeUpdate();
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", true);
            return response;
        } catch (SQLException e) {
            e.printStackTrace();
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", false);
            response.addPayload("error", e.getMessage());
            return response;
        }
    }

    /**
     * Validate login credentials
     */
    public synchronized Message validateLogin(String role, String username, String password) {
        String table = "drivers";
        if (role.equalsIgnoreCase("PASSENGER"))
            table = "passengers";
        else if (role.equalsIgnoreCase("ADMIN"))
            table = "admins";

        String query = "SELECT id FROM " + table + " WHERE username = ? AND password = ?";
        // Only for drivers, check if they are APPROVED
        if (role.equalsIgnoreCase("DRIVER")) {
            query = "SELECT id FROM drivers WHERE username = ? AND password = ? AND status = 'APPROVED'";
        }

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    Message response = new Message(MessageType.DB_RESPONSE);
                    response.addPayload("success", true);
                    response.addPayload("valid", true);
                    response.addPayload("userId", id);
                    response.addPayload("username", username);
                    response.addPayload("role", role);
                    return response;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Login validation error: " + e.getMessage());
        }

        Message response = new Message(MessageType.DB_RESPONSE);
        response.addPayload("success", true);
        response.addPayload("valid", false);
        // Add specific error message for drivers who are pending
        if (role.equalsIgnoreCase("DRIVER")) {
            response.addPayload("error", "Your account is not yet approved. Please wait for admin verification.");
        }
        return response;
    }

    /**
     * Update passenger location
     */
    public synchronized Message updatePassengerLocation(String username, double lat, double lon) {
        String query = "UPDATE passengers SET latitude = ?, longitude = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setDouble(1, lat);
            pstmt.setDouble(2, lon);
            pstmt.setString(3, username);
            pstmt.executeUpdate();

            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", true);
            return response;
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error updating passenger location: " + e.getMessage());
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", false);
            response.addPayload("error", e.getMessage());
            return response;
        }
    }

    /**
     * Update driver location
     */
    public synchronized Message updateDriverLocation(String username, double lat, double lon) {
        String query = "UPDATE drivers SET latitude = ?, longitude = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setDouble(1, lat);
            pstmt.setDouble(2, lon);
            pstmt.setString(3, username);
            pstmt.executeUpdate();

            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", true);
            return response;
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error updating driver location: " + e.getMessage());
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", false);
            response.addPayload("error", e.getMessage());
            return response;
        }
    }

    /**
     * Create a new ride
     */
    public synchronized Message createRide(String passengerUsername, String driverUsername,
            double startLat, double startLon, double destLat, double destLon,
            String startAddr, String destAddr) {
        System.out.println("[DatabaseManager] Creating ride for " + passengerUsername + " (Addr: " + startAddr + " -> "
                + destAddr + ")");

        String query;
        if (driverUsername == null || driverUsername.isEmpty()) {
            query = "INSERT INTO rides (passenger_id, driver_id, start_latitude, start_longitude, dest_latitude, dest_longitude, status, start_address, dest_address) "
                    +
                    "VALUES ((SELECT id FROM passengers WHERE username=?), NULL, ?, ?, ?, ?, 'REQUESTED', ?, ?)";
        } else {
            query = "INSERT INTO rides (passenger_id, driver_id, start_latitude, start_longitude, dest_latitude, dest_longitude, status, start_address, dest_address) "
                    +
                    "VALUES ((SELECT id FROM passengers WHERE username=?), (SELECT id FROM drivers WHERE username=?), ?, ?, ?, ?, 'ASSIGNED', ?, ?)";
        }

        try {
            // Check if passenger exists
            String checkPass = "SELECT id FROM passengers WHERE username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(checkPass)) {
                pstmt.setString(1, passengerUsername);
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    System.err.println("[DatabaseManager] createRide FAIL: Passenger '" + passengerUsername
                            + "' not found in DB.");
                    Message response = new Message(MessageType.DB_RESPONSE);
                    response.addPayload("success", false);
                    response.addPayload("error", "Passenger record not found for username: " + passengerUsername);
                    return response;
                }
            }

            try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, passengerUsername);
                int paramIndex = 2;
                if (driverUsername != null && !driverUsername.isEmpty()) {
                    pstmt.setString(2, driverUsername);
                    paramIndex = 3;
                }
                pstmt.setDouble(paramIndex++, startLat);
                pstmt.setDouble(paramIndex++, startLon);
                pstmt.setDouble(paramIndex++, destLat);
                pstmt.setDouble(paramIndex++, destLon);
                pstmt.setString(paramIndex++, startAddr);
                pstmt.setString(paramIndex++, destAddr);

                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        int rideId = rs.getInt(1);

                        // Insert into ride status history
                        String initStatus = (driverUsername != null && !driverUsername.isEmpty()) ? "ASSIGNED"
                                : "REQUESTED";
                        insertRideStatusHistory(rideId, initStatus, startLat, startLon);

                        Message response = new Message(MessageType.DB_RESPONSE);
                        response.addPayload("success", true);
                        response.addPayload("rideId", rideId);
                        System.out.println("[DatabaseManager] Ride created successfully: ID=" + rideId);
                        return response;
                    }
                } else {
                    System.err.println("[DatabaseManager] createRide: No rows affected. Passenger '" + passengerUsername
                            + "' might not exist.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] createRide SQL Error: " + e.getMessage());
            e.printStackTrace();
            Message err = new Message(MessageType.ERROR);
            err.addPayload("error", "Database Error: " + e.getMessage());
            err.addPayload("success", false);
            return err;
        }

        Message response = new Message(MessageType.DB_RESPONSE);
        response.addPayload("success", false);
        response.addPayload("error", "Failed to create ride");
        return response;
    }

    /**
     * Update ride status
     */
    public synchronized Message assignRideDriver(int rideId, String driverUsername) {
        String query = "UPDATE rides SET driver_id = (SELECT id FROM drivers WHERE username=?), status = 'ASSIGNED' WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, driverUsername);
            pstmt.setInt(2, rideId);

            int rows = pstmt.executeUpdate();

            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", rows > 0);
            return response;
        } catch (SQLException e) {
            e.printStackTrace();
            Message err = new Message(MessageType.ERROR);
            err.addPayload("error", e.getMessage());
            err.addPayload("success", false);
            return err;
        }
    }

    public synchronized Message updateRideStatus(int rideId, String status, double lat, double lon) {
        String query = "UPDATE rides SET status = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, rideId);
            pstmt.executeUpdate();

            // Insert into history
            insertRideStatusHistory(rideId, status, lat, lon);

            // If COMPLETED or CANCELLED, mark driver available again
            if (status.equals("COMPLETED") || status.equals("CANCELLED")) {
                String updateDriver = "UPDATE drivers SET is_available = TRUE WHERE id = (SELECT driver_id FROM rides WHERE id = ?)";
                try (PreparedStatement ps = connection.prepareStatement(updateDriver)) {
                    ps.setInt(1, rideId);
                    ps.executeUpdate();
                }
            }

            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", true);
            return response;
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error updating ride status: " + e.getMessage());
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", false);
            response.addPayload("error", e.getMessage());
            return response;
        }
    }

    /**
     * Insert ride status history
     */
    private void insertRideStatusHistory(int rideId, String status, double lat, double lon) {
        String query = "INSERT INTO ride_status_history (ride_id, status, latitude, longitude) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, rideId);
            pstmt.setString(2, status);
            pstmt.setDouble(3, lat);
            pstmt.setDouble(4, lon);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error inserting ride status history: " + e.getMessage());
        }
    }

    /**
     * Get active rides (for web map)
     */
    public synchronized Message getActiveRides() {
        String query = "SELECT r.id, p.username as passenger, d.username as driver, " +
                "p.latitude as p_lat, p.longitude as p_lon, " +
                "d.latitude as d_lat, d.longitude as d_lon, r.status " +
                "FROM rides r " +
                "JOIN passengers p ON r.passenger_id = p.id " +
                "JOIN drivers d ON r.driver_id = d.id " +
                "WHERE r.status IN ('ASSIGNED', 'STARTED')";

        StringBuilder ridesJson = new StringBuilder("[");

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            boolean first = true;
            while (rs.next()) {
                if (!first)
                    ridesJson.append(",");
                ridesJson.append("{");
                ridesJson.append("\"rideId\":").append(rs.getInt("id")).append(",");
                ridesJson.append("\"passenger\":\"").append(rs.getString("passenger")).append("\",");
                ridesJson.append("\"driver\":\"").append(rs.getString("driver")).append("\",");
                ridesJson.append("\"p_lat\":").append(rs.getDouble("p_lat")).append(",");
                ridesJson.append("\"p_lon\":").append(rs.getDouble("p_lon")).append(",");
                ridesJson.append("\"d_lat\":").append(rs.getDouble("d_lat")).append(",");
                ridesJson.append("\"d_lon\":").append(rs.getDouble("d_lon")).append(",");
                ridesJson.append("\"status\":\"").append(rs.getString("status")).append("\"");
                ridesJson.append("}");
                first = false;
            }

            ridesJson.append("]");

            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", true);
            response.addPayload("rides", ridesJson.toString());
            return response;

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error getting active rides: " + e.getMessage());
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", false);
            response.addPayload("error", e.getMessage());
            return response;
        }
    }

    /**
     * Get all locations for web map
     */
    public synchronized Message getAllLocations() {
        StringBuilder json = new StringBuilder("{");

        try (Statement stmt = connection.createStatement()) {
            // Get passengers
            json.append("\"passengers\":[");
            ResultSet rs = stmt
                    .executeQuery("SELECT username, latitude, longitude FROM passengers WHERE latitude IS NOT NULL");
            boolean first = true;
            while (rs.next()) {
                if (!first)
                    json.append(",");
                json.append("{\"username\":\"").append(rs.getString("username")).append("\",");
                json.append("\"lat\":").append(rs.getDouble("latitude")).append(",");
                json.append("\"lng\":").append(rs.getDouble("longitude")).append("}");
                first = false;
            }
            json.append("],");

            // Get drivers
            json.append("\"drivers\":[");
            rs = stmt.executeQuery(
                    "SELECT username, latitude, longitude, is_available FROM drivers WHERE latitude IS NOT NULL");
            first = true;
            while (rs.next()) {
                if (!first)
                    json.append(",");
                json.append("{\"username\":\"").append(rs.getString("username")).append("\",");
                json.append("\"lat\":").append(rs.getDouble("latitude")).append(",");
                json.append("\"lng\":").append(rs.getDouble("longitude")).append(",");
                json.append("\"available\":").append(rs.getBoolean("is_available")).append("}");
                first = false;
            }
            json.append("],");

            // Get active rides (REQUESTED, ASSIGNED, STARTED)
            json.append("\"rides\":[");
            rs = stmt.executeQuery(
                    "SELECT r.id, p.username as passenger, r.status, r.start_latitude, r.start_longitude, r.dest_latitude, r.dest_longitude FROM rides r JOIN passengers p ON r.passenger_id = p.id WHERE r.status IN ('REQUESTED', 'ASSIGNED', 'STARTED')");
            first = true;
            while (rs.next()) {
                if (!first)
                    json.append(",");
                json.append("{\"id\":").append(rs.getInt("id")).append(",");
                json.append("\"passenger\":\"").append(rs.getString("passenger")).append("\",");
                json.append("\"p_lat\":").append(rs.getDouble("start_latitude")).append(",");
                json.append("\"p_lon\":").append(rs.getDouble("start_longitude")).append(",");
                json.append("\"d_lat\":").append(rs.getDouble("dest_latitude")).append(",");
                json.append("\"d_lon\":").append(rs.getDouble("dest_longitude")).append(",");
                json.append("\"status\":\"").append(rs.getString("status")).append("\"}");
                first = false;
            }
            json.append("]}");

            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", true);
            response.addPayload("locations", json.toString());
            return response;

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error getting locations: " + e.getMessage());
            Message response = new Message(MessageType.DB_RESPONSE);
            response.addPayload("success", false);
            response.addPayload("error", e.getMessage());
            return response;
        }
    }

    /**
     * Export all data for synchronization
     */
    public synchronized Message getAllTableData() {
        Message response = new Message(MessageType.SYNC_DATA_RESPONSE);
        try (Statement stmt = connection.createStatement()) {

            // Passengers
            StringBuilder passJson = new StringBuilder("[");
            ResultSet rs = stmt.executeQuery("SELECT * FROM passengers");
            boolean first = true;
            while (rs.next()) {
                if (!first)
                    passJson.append(",");
                passJson.append("{\"username\":\"").append(rs.getString("username")).append("\",");
                passJson.append("\"password\":\"").append(rs.getString("password")).append("\",");
                passJson.append("\"phone\":\"").append(rs.getString("phone")).append("\"}");
                first = false;
            }
            passJson.append("]");

            // Drivers
            StringBuilder drivJson = new StringBuilder("[");
            rs = stmt.executeQuery("SELECT * FROM drivers");
            first = true;
            while (rs.next()) {
                if (!first)
                    drivJson.append(",");
                drivJson.append("{\"username\":\"").append(rs.getString("username")).append("\",");
                drivJson.append("\"password\":\"").append(rs.getString("password")).append("\",");
                drivJson.append("\"phone\":\"").append(rs.getString("phone")).append("\"}");
                first = false;
            }
            drivJson.append("]");

            // Rides (Simplified for sync)
            StringBuilder rideJson = new StringBuilder("[");
            // Note: properly exporting rides requires more fields, but for this demo
            // ensuring users exist is critical
            // We skip rides deep sync for brevity in this method, or implement if critical.
            // Let's rely on users existing first.
            rideJson.append("]");

            response.addPayload("passengers", passJson.toString());
            response.addPayload("drivers", drivJson.toString());
            response.addPayload("success", true);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(MessageType.ERROR);
        }
    }

    /**
     * Import data from another server
     */
    public synchronized void importData(String passJson, String drivJson) {
        try {
            // Very basic JSON parsing (regex) to avoid heavy libs
            // Insert Passengers
            if (passJson != null && passJson.length() > 2) {
                // Remove [ ]
                String content = passJson.substring(1, passJson.length() - 1);
                String[] items = content.split("\\},\\{"); // loose split
                for (String item : items) {
                    item = item.replace("{", "").replace("}", "").replace("\"", "");
                    Map<String, String> map = new HashMap<>();
                    for (String pair : item.split(",")) {
                        String[] kv = pair.split(":");
                        if (kv.length == 2)
                            map.put(kv[0], kv[1]);
                    }
                    if (map.containsKey("username")) {
                        registerPassenger(map.get("username"), map.get("password"), map.get("phone"));
                    }
                }
            }
            // Same for drivers... (Implementation simplified for brevity)
            if (drivJson != null && drivJson.length() > 2) {
                String content = drivJson.substring(1, drivJson.length() - 1);
                String[] items = content.split("\\},\\{");
                for (String item : items) {
                    item = item.replace("{", "").replace("}", "").replace("\"", "");
                    Map<String, String> map = new HashMap<>();
                    for (String pair : item.split(",")) {
                        String[] kv = pair.split(":");
                        if (kv.length == 2)
                            map.put(kv[0], kv[1]);
                    }
                    if (map.containsKey("username")) {
                        registerDriver(map.get("username"), map.get("password"), map.get("phone"));
                    }
                }
            }
            System.out.println("[DatabaseManager] Data Import Complete");
        } catch (Exception e) {
            System.err.println("Import failed: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DatabaseManager] Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error closing connection: " + e.getMessage());
        }
    }
}
