package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class PassengerGUI extends JFrame {
  private static final String SERVER_ADDRESS = "localhost";
  private static final int SERVER_PORT = 5000;

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  // UI Components
  private JTextField usernameField;
  private JButton loginButton;
  private JTextField latField, lonField;
  private JButton updateLocButton;
  private JButton requestRideButton;
  private JTextArea logArea;
  private JLabel statusLabel;

  public PassengerGUI() {
    super("Passenger Client");
    setSize(400, 500);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    // Top Panel: Login
    JPanel topPanel = new JPanel(new GridLayout(2, 2));
    topPanel.add(new JLabel("Username:"));
    usernameField = new JTextField("Alice");
    topPanel.add(usernameField);
    loginButton = new JButton("Login");
    topPanel.add(loginButton);
    statusLabel = new JLabel("Not Connected");
    topPanel.add(statusLabel);
    add(topPanel, BorderLayout.NORTH);

    // Center Panel: Controls
    JPanel centerPanel = new JPanel(new GridLayout(4, 2, 5, 5));
    centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    centerPanel.add(new JLabel("Latitude:"));
    latField = new JTextField("9.02");
    centerPanel.add(latField);

    centerPanel.add(new JLabel("Longitude:"));
    lonField = new JTextField("38.74");
    centerPanel.add(lonField);

    updateLocButton = new JButton("Update Location");
    updateLocButton.setEnabled(false);
    centerPanel.add(updateLocButton);

    requestRideButton = new JButton("Request Ride");
    requestRideButton.setEnabled(false);
    centerPanel.add(requestRideButton);

    add(centerPanel, BorderLayout.CENTER);

    // Bottom Panel: Logs
    logArea = new JTextArea();
    logArea.setEditable(false);
    add(new JScrollPane(logArea), BorderLayout.SOUTH);

    // Actions
    loginButton.addActionListener(e -> connectAndLogin());
    updateLocButton.addActionListener(e -> sendLocation());
    requestRideButton.addActionListener(e -> requestRide());

    setVisible(true);
  }

  private void connectAndLogin() {
    try {
      socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      // Start Reader Thread
      new Thread(this::listenToServer).start();

      // Send Login
      String username = usernameField.getText();
      out.println("LOGIN PASSENGER " + username);

      loginButton.setEnabled(false);
      usernameField.setEditable(false);
      updateLocButton.setEnabled(true);
      requestRideButton.setEnabled(true);
      statusLabel.setText("Connected: " + username);

    } catch (IOException e) {
      log("Connection Failed: " + e.getMessage());
    }
  }

  private void listenToServer() {
    try {
      String message;
      while ((message = in.readLine()) != null) {
        log("[SERVER]: " + message);
        // Handle popups based on messages
        if (message.startsWith("RIDE_ASSIGNED")) {
          JOptionPane.showMessageDialog(this, "Ride Found!\n" + message);
        }
      }
    } catch (IOException e) {
      log("Disconnected.");
    }
  }

  private void sendLocation() {
    String lat = latField.getText();
    String lon = lonField.getText();
    out.println("LOC " + lat + " " + lon);
  }

  private void requestRide() {
    String lat = latField.getText();
    String lon = lonField.getText();
    out.println("REQUEST_RIDE " + lat + " " + lon);
    log("Request sent...");
  }

  private void log(String msg) {
    SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(PassengerGUI::new);
  }
}
