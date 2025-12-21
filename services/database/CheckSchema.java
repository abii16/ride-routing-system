package services.database;

import java.sql.*;
import java.io.FileInputStream;
import java.util.Properties;

public class CheckSchema {
  public static void main(String[] args) {
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
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getColumns(null, null, "drivers", null);
        System.out.println("Columns in 'drivers' table:");
        while (rs.next()) {
          System.out.println("- " + rs.getString("COLUMN_NAME") + " (" + rs.getString("TYPE_NAME") + ")");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
