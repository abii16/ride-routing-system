package clients.passenger;

import common.Message;
import common.MessageType;
import common.JSONUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

/**
 * Passenger Client GUI - Connects to Dispatch Server
 */
public class PassengerClientGUI extends JFrame {
    private static final String DISPATCH_HOST = "localhost";
    private static final int DISPATCH_PORT = 5000;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean connected = false;
    
    // GUI Components
    private JTextField usernameField, passwordField, phoneField;
    private JTextField pickupLatField, pickupLonField, destLatField, destLonField;
    private JButton loginButton, registerButton, requestRideButton, updateLocationButton;
    private JTextArea statusArea;
    private JLabel connectionStatus;
    
    public PassengerClientGUI() {
        setTitle("Passenger Client - Distributed Ride Sharing");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Create UI
        createLoginPanel();
        createRidePanel();
        createStatusPanel();
        
        setVisible(true);
        
        // Connect to server
        connectToServer();
        
        // Start message listener thread
        new Thread(this::listenForMessages).start();
    }
    
    private void createLoginPanel() {
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new GridLayout(5, 2, 5, 5));
        loginPanel.setBorder(BorderFactory.createTitledBorder("Authentication"));
        
        loginPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        loginPanel.add(usernameField);
        
        loginPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        loginPanel.add(passwordField);
        
        loginPanel.add(new JLabel("Phone:"));
        phoneField = new JTextField();
        loginPanel.add(phoneField);
        
        loginButton = new JButton("Login");
        loginButton.addActionListener(e -> login());
        loginPanel.add(loginButton);
        
        registerButton = new JButton("Register");
        registerButton.addActionListener(e -> register());
        loginPanel.add(registerButton);
        
        connectionStatus = new JLabel("Status: Connecting...");
        loginPanel.add(connectionStatus);
        
        add(loginPanel, BorderLayout.NORTH);
    }
    
    private void createRidePanel() {
        JPanel ridePanel = new JPanel();
        ridePanel.setLayout(new GridLayout(6, 2, 5, 5));
        ridePanel.setBorder(BorderFactory.createTitledBorder("Ride Request"));
        
        ridePanel.add(new JLabel("Pickup Latitude:"));
        pickupLatField = new JTextField("9.0054");
        ridePanel.add(pickupLatField);
        
        ridePanel.add(new JLabel("Pickup Longitude:"));
        pickupLonField = new JTextField("38.7636");
        ridePanel.add(pickupLonField);
        
        ridePanel.add(new JLabel("Destination Latitude:"));
        destLatField = new JTextField("9.0100");
        ridePanel.add(destLatField);
        
        ridePanel.add(new JLabel("Destination Longitude:"));
        destLonField = new JTextField("38.7700");
        ridePanel.add(destLonField);
        
        updateLocationButton = new JButton("Update My Location");
        updateLocationButton.addActionListener(e -> updateLocation());
        updateLocationButton.setEnabled(false);
        ridePanel.add(updateLocationButton);
        
        requestRideButton = new JButton("Request Ride");
        requestRideButton.addActionListener(e -> requestRide());
        requestRideButton.setEnabled(false);
        ridePanel.add(requestRideButton);
        
        add(ridePanel, BorderLayout.CENTER);
    }
    
    private void createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(statusArea);
        
        statusPanel.add(scrollPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void connectToServer() {
        try {
            socket = new Socket(DISPATCH_HOST, DISPATCH_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            connectionStatus.setText("Status: Connected to Dispatch Server");
            connectionStatus.setForeground(Color.GREEN);
            log("Connected to Dispatch Server at " + DISPATCH_HOST + ":" + DISPATCH_PORT);
        } catch (IOException e) {
            connectionStatus.setText("Status: Connection Failed");
            connectionStatus.setForeground(Color.RED);
            log("ERROR: Failed to connect to Dispatch Server: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Failed to connect to Dispatch Server!\nMake sure the server is running.",
                "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void login() {
        if (!connected) {
            JOptionPane.showMessageDialog(this, "Not connected to server!");
            return;
        }
        
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();
        
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password!");
            return;
        }
        
        Message message = new Message(MessageType.LOGIN);
        message.addPayload("username", user);
        message.addPayload("password", pass);
        
        sendMessage(message);
        log("Sent login request for user: " + user);
    }
    
    private void register() {
        if (!connected) {
            JOptionPane.showMessageDialog(this, "Not connected to server!");
            return;
        }
        
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();
        String phone = phoneField.getText().trim();
        
        if (user.isEmpty() || pass.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!");
            return;
        }
        
        Message message = new Message(MessageType.REGISTER_PASSENGER);
        message.addPayload("username", user);
        message.addPayload("password", pass);
        message.addPayload("phone", phone);
        
        sendMessage(message);
        log("Sent registration request for user: " + user);
    }
    
    private void updateLocation() {
        try {
            double lat = Double.parseDouble(pickupLatField.getText());
            double lon = Double.parseDouble(pickupLonField.getText());
            
            Message message = new Message(MessageType.UPDATE_LOCATION);
            message.addPayload("latitude", lat);
            message.addPayload("longitude", lon);
            
            sendMessage(message);
            log("Updated location: (" + lat + ", " + lon + ")");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid coordinates!");
        }
    }
    
    private void requestRide() {
        try {
            double pickupLat = Double.parseDouble(pickupLatField.getText());
            double pickupLon = Double.parseDouble(pickupLonField.getText());
            double destLat = Double.parseDouble(destLatField.getText());
            double destLon = Double.parseDouble(destLonField.getText());
            
            Message message = new Message(MessageType.RIDE_REQUEST);
            message.addPayload("pickupLat", pickupLat);
            message.addPayload("pickupLon", pickupLon);
            message.addPayload("destLat", destLat);
            message.addPayload("destLon", destLon);
            
            sendMessage(message);
            log("Sent ride request from (" + pickupLat + ", " + pickupLon + ") to (" + destLat + ", " + destLon + ")");
            requestRideButton.setEnabled(false);
            log("Waiting for driver assignment...");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid coordinates!");
        }
    }
    
    private void sendMessage(Message message) {
        if (out != null) {
            String json = JSONUtil.toJSON(message);
            out.println(json);
        }
    }
    
    private void listenForMessages() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                Message message = JSONUtil.fromJSON(line);
                if (message != null) {
                    handleServerMessage(message);
                }
            }
        } catch (IOException e) {
            if (connected) {
                log("ERROR: Connection lost: " + e.getMessage());
                connectionStatus.setText("Status: Disconnected");
                connectionStatus.setForeground(Color.RED);
            }
        }
    }
    
    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case LOGIN_SUCCESS:
                    username = usernameField.getText();
                    log("LOGIN SUCCESS: Welcome " + username);
                    loginButton.setEnabled(false);
                    registerButton.setEnabled(false);
                    requestRideButton.setEnabled(true);
                    updateLocationButton.setEnabled(true);
                    break;
                    
                case LOGIN_FAILED:
                    String error = message.getPayloadString("error");
                    log("LOGIN FAILED: " + error);
                    JOptionPane.showMessageDialog(this, "Login failed: " + error);
                    break;
                    
                case LOCATION_UPDATED:
                    log("Location updated successfully");
                    break;
                    
                case RIDE_ASSIGNMENT:
                    String driver = message.getPayloadString("driverUsername");
                    int rideId = message.getPayloadInt("rideId");
                    log("RIDE ASSIGNED! Driver: " + driver + " | Ride ID: " + rideId);
                    JOptionPane.showMessageDialog(this, 
                        "Driver assigned!\nDriver: " + driver + "\nRide ID: " + rideId,
                        "Ride Assigned", JOptionPane.INFORMATION_MESSAGE);
                    requestRideButton.setEnabled(true);
                    break;
                    
                case NO_DRIVERS_AVAILABLE:
                    log("NO DRIVERS AVAILABLE - Please try again later");
                    JOptionPane.showMessageDialog(this, 
                        "No drivers available at the moment. Please try again later.",
                        "No Drivers", JOptionPane.WARNING_MESSAGE);
                    requestRideButton.setEnabled(true);
                    break;
                    
                case ERROR:
                    String errorMsg = message.getPayloadString("error");
                    log("ERROR: " + errorMsg);
                    break;
                    
                default:
                    log("Received: " + message.getType());
            }
        });
    }
    
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            statusArea.append("[" + timestamp + "] " + msg + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(PassengerClientGUI::new);
    }
}
