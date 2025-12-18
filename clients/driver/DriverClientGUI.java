package clients.driver;

import common.Message;
import common.MessageType;
import common.JSONUtil;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Driver Client GUI - Connects to Driver Service
 */
public class DriverClientGUI extends JFrame {
    private static final String DRIVER_SERVICE_HOST = "localhost";
    private static final int DRIVER_SERVICE_PORT = 5001;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean connected = false;
    private boolean available = true;
    
    // Location update timer
    private Timer locationTimer;
    
    // GUI Components
    private JTextField usernameField, passwordField, phoneField;
    private JTextField latField, lonField;
    private JCheckBox availableCheckbox;
    private JButton loginButton, registerButton, updateLocationButton;
    private JTextArea statusArea;
    private JLabel connectionStatus, availabilityStatus;
    
    public DriverClientGUI() {
        setTitle("Driver Client - Distributed Ride Sharing");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Create UI
        createLoginPanel();
        createDriverPanel();
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
    
    private void createDriverPanel() {
        JPanel driverPanel = new JPanel();
        driverPanel.setLayout(new GridLayout(5, 2, 5, 5));
        driverPanel.setBorder(BorderFactory.createTitledBorder("Driver Controls"));
        
        driverPanel.add(new JLabel("Current Latitude:"));
        latField = new JTextField("9.0060");
        driverPanel.add(latField);
        
        driverPanel.add(new JLabel("Current Longitude:"));
        lonField = new JTextField("38.7640");
        driverPanel.add(lonField);
        
        updateLocationButton = new JButton("Update Location Now");
        updateLocationButton.addActionListener(e -> updateLocation());
        updateLocationButton.setEnabled(false);
        driverPanel.add(updateLocationButton);
        
        availableCheckbox = new JCheckBox("Available for Rides");
        availableCheckbox.setSelected(true);
        availableCheckbox.addActionListener(e -> toggleAvailability());
        availableCheckbox.setEnabled(false);
        driverPanel.add(availableCheckbox);
        
        availabilityStatus = new JLabel("Status: Not logged in");
        availabilityStatus.setFont(new Font("Arial", Font.BOLD, 14));
        driverPanel.add(availabilityStatus);
        
        add(driverPanel, BorderLayout.CENTER);
    }
    
    private void createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(statusArea);
        
        statusPanel.add(scrollPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void connectToServer() {
        try {
            socket = new Socket(DRIVER_SERVICE_HOST, DRIVER_SERVICE_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            connectionStatus.setText("Status: Connected to Driver Service");
            connectionStatus.setForeground(Color.GREEN);
            log("Connected to Driver Service at " + DRIVER_SERVICE_HOST + ":" + DRIVER_SERVICE_PORT);
        } catch (IOException e) {
            connectionStatus.setText("Status: Connection Failed");
            connectionStatus.setForeground(Color.RED);
            log("ERROR: Failed to connect to Driver Service: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Failed to connect to Driver Service!\nMake sure the service is running.",
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
        
        // For drivers, we register directly with location
        double lat = Double.parseDouble(latField.getText());
        double lon = Double.parseDouble(lonField.getText());
        
        Message message = new Message(MessageType.REGISTER_DRIVER);
        message.addPayload("username", user);
        message.addPayload("latitude", lat);
        message.addPayload("longitude", lon);
        
        sendMessage(message);
        log("Sent registration/login request for driver: " + user);
    }
    
    private void register() {
        login(); // Same process for drivers
    }
    
    private void updateLocation() {
        try {
            double lat = Double.parseDouble(latField.getText());
            double lon = Double.parseDouble(lonField.getText());
            
            Message message = new Message(MessageType.UPDATE_LOCATION);
            message.addPayload("latitude", lat);
            message.addPayload("longitude", lon);
            
            sendMessage(message);
            log("Updated location: (" + lat + ", " + lon + ")");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid coordinates!");
        }
    }
    
    private void toggleAvailability() {
        available = availableCheckbox.isSelected();
        
        Message message = new Message(MessageType.UPDATE_AVAILABILITY);
        message.addPayload("available", available);
        
        sendMessage(message);
        log("Availability changed to: " + (available ? "AVAILABLE" : "BUSY"));
        
        updateAvailabilityStatus();
    }
    
    private void updateAvailabilityStatus() {
        SwingUtilities.invokeLater(() -> {
            if (available) {
                availabilityStatus.setText("Status: AVAILABLE for rides");
                availabilityStatus.setForeground(Color.GREEN);
            } else {
                availabilityStatus.setText("Status: BUSY");
                availabilityStatus.setForeground(Color.RED);
            }
        });
    }
    
    private void startLocationUpdates() {
        // Send location updates every 3 seconds
        locationTimer = new Timer();
        locationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (connected && username != null) {
                    updateLocation();
                }
            }
        }, 3000, 3000);
        
        log("Started automatic location updates (every 3 seconds)");
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
                    updateLocationButton.setEnabled(true);
                    availableCheckbox.setEnabled(true);
                    updateAvailabilityStatus();
                    startLocationUpdates();
                    break;
                    
                case LOCATION_UPDATED:
                    // Silent update
                    break;
                    
                case AVAILABILITY_UPDATED:
                    boolean isAvailable = message.getPayloadBoolean("available");
                    log("Availability confirmed: " + (isAvailable ? "AVAILABLE" : "BUSY"));
                    break;
                    
                case RIDE_ASSIGNMENT:
                    handleRideAssignment(message);
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
    
    private void handleRideAssignment(Message message) {
        int rideId = message.getPayloadInt("rideId");
        String passenger = message.getPayloadString("passengerUsername");
        double pickupLat = message.getPayloadDouble("pickupLat");
        double pickupLon = message.getPayloadDouble("pickupLon");
        
        log("========================================");
        log("NEW RIDE ASSIGNMENT!");
        log("Ride ID: " + rideId);
        log("Passenger: " + passenger);
        log("Pickup Location: (" + pickupLat + ", " + pickupLon + ")");
        log("========================================");
        
        // Mark as busy
        available = false;
        availableCheckbox.setSelected(false);
        updateAvailabilityStatus();
        
        // Show dialog
        int response = JOptionPane.showConfirmDialog(this,
            "New Ride Request!\n\n" +
            "Ride ID: " + rideId + "\n" +
            "Passenger: " + passenger + "\n" +
            "Pickup: (" + pickupLat + ", " + pickupLon + ")\n\n" +
            "Accept this ride?",
            "New Ride Assignment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
        
        if (response == JOptionPane.YES_OPTION) {
            acceptRide(rideId);
        } else {
            rejectRide(rideId);
        }
    }
    
    private void acceptRide(int rideId) {
        Message message = new Message(MessageType.RIDE_ACCEPTED);
        message.addPayload("rideId", rideId);
        sendMessage(message);
        log("Ride " + rideId + " ACCEPTED");
        
        // Ask if they want to start the ride now
        int start = JOptionPane.showConfirmDialog(this,
            "Start the ride now?",
            "Start Ride",
            JOptionPane.YES_NO_OPTION);
        
        if (start == JOptionPane.YES_OPTION) {
            startRide(rideId);
        }
    }
    
    private void rejectRide(int rideId) {
        Message message = new Message(MessageType.RIDE_REJECTED);
        message.addPayload("rideId", rideId);
        sendMessage(message);
        log("Ride " + rideId + " REJECTED");
        
        // Mark as available again
        available = true;
        availableCheckbox.setSelected(true);
        updateAvailabilityStatus();
    }
    
    private void startRide(int rideId) {
        try {
            double lat = Double.parseDouble(latField.getText());
            double lon = Double.parseDouble(lonField.getText());
            
            Message message = new Message(MessageType.RIDE_STARTED);
            message.addPayload("rideId", rideId);
            message.addPayload("latitude", lat);
            message.addPayload("longitude", lon);
            
            sendMessage(message);
            log("Ride " + rideId + " STARTED");
            
            // Ask when completed
            Timer completeTimer = new Timer();
            completeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> {
                        int complete = JOptionPane.showConfirmDialog(DriverClientGUI.this,
                            "Is the ride complete?",
                            "Complete Ride",
                            JOptionPane.YES_NO_OPTION);
                        
                        if (complete == JOptionPane.YES_OPTION) {
                            completeRide(rideId);
                            this.cancel();
                        }
                    });
                }
            }, 10000, 10000); // Ask every 10 seconds
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid coordinates!");
        }
    }
    
    private void completeRide(int rideId) {
        try {
            double lat = Double.parseDouble(latField.getText());
            double lon = Double.parseDouble(lonField.getText());
            
            Message message = new Message(MessageType.RIDE_COMPLETED);
            message.addPayload("rideId", rideId);
            message.addPayload("latitude", lat);
            message.addPayload("longitude", lon);
            
            sendMessage(message);
            log("Ride " + rideId + " COMPLETED");
            
            // Mark as available again
            available = true;
            availableCheckbox.setSelected(true);
            updateAvailabilityStatus();
            
            JOptionPane.showMessageDialog(this, "Ride completed successfully!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid coordinates!");
        }
    }
    
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            statusArea.append("[" + timestamp + "] " + msg + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(DriverClientGUI::new);
    }
}
