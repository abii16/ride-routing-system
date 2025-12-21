package services.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.io.FileInputStream;
import java.util.Properties;

public class ResetSchema {
  public static void main(String[] args) {
    System.out.println("Resetting Driver Table Schema...");
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
        stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
        stmt.executeUpdate("DROP TABLE IF EXISTS drivers");
        stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        System.out.println("SUCCESS: Table 'drivers' dropped. Restart the server to recreate it.");

      }
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("FAILED: " + e.getMessage());
    }
  }
}
