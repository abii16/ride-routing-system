package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class DriverClient {
  private static final String SERVER_ADDRESS = "localhost";
  private static final int SERVER_PORT = 5000;

  public static void main(String[] args) {
    System.out.println("Starting Driver Client...");
    try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        Scanner scanner = new Scanner(System.in)) {
      System.out.println("Connected to RideSharingServer at " + SERVER_ADDRESS + ":" + SERVER_PORT);

      // Thread to listen for server messages
      new Thread(() -> {
        try {
          String serverMessage;
          while ((serverMessage = in.readLine()) != null) {
            System.out.println("\n[SERVER]: " + serverMessage);
          }
        } catch (IOException e) {
          System.out.println("Disconnected from server.");
          System.exit(0);
        }
      }).start();

      // Main interaction loop
      System.out.println("LOGGING IN...");
      System.out.print("Enter your Username: ");
      if (scanner.hasNextLine()) {
        String username = scanner.nextLine();
        out.println("LOGIN DRIVER " + username);
      }

      // Command Loop
      while (true) {
        // Formatting menu
        Thread.sleep(500);
        System.out.println("\n--- DRIVER MENU ---");
        System.out.println("1. Update Location");
        System.out.println("2. Check for Rides (Coming Soon)");
        System.out.println("3. Toggle Availability (Coming Soon)");
        System.out.println("4. Exit");
        System.out.print("Select: ");

        if (scanner.hasNextLine()) {
          String choice = scanner.nextLine();
          if (choice.equals("1")) {
            System.out.print("Enter Latitude (e.g. 9.00): ");
            String lat = scanner.nextLine();
            System.out.print("Enter Longitude (e.g. 38.00): ");
            String lon = scanner.nextLine();
            out.println("LOC " + lat + " " + lon);
          } else if (choice.equals("4")) {
            break;
          }
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
