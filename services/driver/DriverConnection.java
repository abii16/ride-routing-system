package services.driver;

import common.Message;
import common.MessageType;
import common.JSONUtil;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Driver Information class - holds driver state
 */
class DriverInfo {
    private String username;
    private double latitude;
    private double longitude;
    private boolean available;
    private long lastUpdate;
    
    public DriverInfo(String username) {
        this.username = username;
        this.available = true;
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public String getUsername() { return username; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isAvailable() { return available; }
    public long getLastUpdate() { return lastUpdate; }
    
    public void setLocation(double lat, double lon) {
        this.latitude = lat;
        this.longitude = lon;
        this.lastUpdate = System.currentTimeMillis();
    }
    
public void setAvailable(boolean available) {
        this.available = available;
        this.lastUpdate = System.currentTimeMillis();
    }
}

/**
 * Driver Connection - represents a connected driver client
 */
class DriverConnection implements Runnable {
    private Socket socket;
    private String username;
    private BufferedReader in;
    private PrintWriter out;
    private DriverServiceServer server;
    private volatile boolean running = true;
    private boolean isWebClient = false;
    
    public DriverConnection(Socket socket, DriverServiceServer server) {
        this.socket = socket;
        this.server = server;
    }


    
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            System.out.println("[DriverConnection] New driver connection from: " + socket.getInetAddress());
            
            // Handle messages from driver
            String line;
            while (running && (line = in.readLine()) != null) {
                handleMessage(line);
            }
            
        } catch (IOException e) {
            System.err.println("[DriverConnection] Connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void handleMessage(String json) {
        try {
            Message message = JSONUtil.fromJSON(json);
            if (message == null) {
                sendError("Invalid message format");
                return;
            }
            
            System.out.println("[DriverConnection] Received " + message.getType() + " from " + username);
            
            switch (message.getType()) {
                case REGISTER_DRIVER:
                    handleRegister(message);
                    break;
                    
                case UPDATE_LOCATION:
                    handleLocationUpdate(message);
                    break;
                    
                case UPDATE_AVAILABILITY:
                    handleAvailabilityUpdate(message);
                    break;
                    
                case RIDE_ACCEPTED:
                    handleRideAccepted(message);
                    break;
                    
                case RIDE_STARTED:
                    handleRideStarted(message);
                    break;
                    
                case RIDE_COMPLETED:
                    handleRideCompleted(message);
                    break;
                    
                case DISCONNECT:
                    running = false;
                    break;
                    
                default:
                    sendError("Unknown message type: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("[DriverConnection] Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleRegister(Message message) {
        this.username = message.getPayloadString("username");
        double lat = message.getPayloadDouble("latitude");
        double lon = message.getPayloadDouble("longitude");
        
        // Check if this is a stateless web client
        Object webFlag = message.getPayloadValue("isWebClient");
        if (webFlag != null && (Boolean)webFlag) {
            this.isWebClient = true;
            System.out.println("[DriverConnection] Web Client detected: " + username);
        }
        
        server.registerDriver(username, lat, lon, this);
        
        Message response = new Message(MessageType.LOGIN_SUCCESS);
        response.addPayload("username", username);
        response.addPayload("message", "Driver registered successfully");
        sendMessage(response);
        
        System.out.println("[DriverConnection] Driver registered: " + username);
    }
    
    private void handleLocationUpdate(Message message) {
        double lat = message.getPayloadDouble("latitude");
        double lon = message.getPayloadDouble("longitude");
        
        server.updateDriverLocation(username, lat, lon);
        
        Message response = new Message(MessageType.LOCATION_UPDATED);
        sendMessage(response);
    }
    
    private void handleAvailabilityUpdate(Message message) {
        boolean available = message.getPayloadBoolean("available");
        server.updateDriverAvailability(username, available);
        
        Message response = new Message(MessageType.AVAILABILITY_UPDATED);
        response.addPayload("available", available);
        sendMessage(response);
        
        System.out.println("[DriverConnection] Driver " + username + " availability: " + available);
    }
    
    private void handleRideAccepted(Message message) {
        int rideId = message.getPayloadInt("rideId");
        server.notifyRideAccepted(username, rideId);
        System.out.println("[DriverConnection] Driver " + username + " accepted ride " + rideId);
    }
    
    private void handleRideStarted(Message message) {
        int rideId = message.getPayloadInt("rideId");
        double lat = message.getPayloadDouble("latitude");
        double lon = message.getPayloadDouble("longitude");
        server.notifyRideStarted(username, rideId, lat, lon);
        System.out.println("[DriverConnection] Driver " + username + " started ride " + rideId);
    }
    
    private void handleRideCompleted(Message message) {
        int rideId = message.getPayloadInt("rideId");
        double lat = message.getPayloadDouble("latitude");
        double lon = message.getPayloadDouble("longitude");
        server.notifyRideCompleted(username, rideId, lat, lon);
        System.out.println("[DriverConnection] Driver " + username + " completed ride " + rideId);
    }
    
    public void sendMessage(Message message) {
        if (out != null) {
            String json = JSONUtil.toJSON(message);
            out.println(json);
        }
    }
    
    private void sendError(String errorMsg) {
        Message error = new Message(MessageType.ERROR);
        error.addPayload("error", errorMsg);
        sendMessage(error);
    }
    
    public String getUsername() {
        return username;
    }
    
    private void cleanup() {
        running = false;
        // Only unregister if NOT a web client
        if (username != null && !isWebClient) {
            server.unregisterDriver(username);
            System.out.println("[DriverConnection] Driver disconnected: " + username);
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
