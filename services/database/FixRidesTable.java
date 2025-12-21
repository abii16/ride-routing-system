package services.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.io.FileInputStream;
import java.util.Properties;

public class FixRidesTable {
  public static void main(String[] args) {
    System.out.println("Fixing Rides Table Schema...");
    String DB_HOST = "localhost";

    try (java.io.InputStream input = new FileInputStream("config.properties")) {
      Properties prop = new Properties();
      prop.load(input);
      String h = prop.getProperty("MYSQL_HOST");
      if (h != null && !h.isEmpty())
        DB_HOST = h;
    } catch (Exception e) {
    }

    String URL = "jdbc:mysql://" + DB_HOST + ":3306/ride_sharing_distributed";
    String USER = "root";
    String PASSWORD = "";

    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
          Statement stmt = conn.createStatement()) {

        System.out.println("Altering columns in 'rides' table...");

        try {
          stmt.executeUpdate("ALTER TABLE rides CHANGE COLUMN start_lat start_latitude DOUBLE NOT NULL");
        } catch (Exception e) {
        }
        try {
          stmt.executeUpdate("ALTER TABLE rides CHANGE COLUMN start_lon start_longitude DOUBLE NOT NULL");
        } catch (Exception e) {
        }
        try {
          stmt.executeUpdate("ALTER TABLE rides CHANGE COLUMN dest_lat dest_latitude DOUBLE NOT NULL");
        } catch (Exception e) {
        }
        try {
          stmt.executeUpdate("ALTER TABLE rides CHANGE COLUMN dest_lon dest_longitude DOUBLE NOT NULL");
        } catch (Exception e) {
        }

        System.out.println("SUCCESS: Columns updated.");
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("FAILED: " + e.getMessage());
    }
  }
}
