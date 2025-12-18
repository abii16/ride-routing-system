package services.driver;

import common.Message;
import common.MessageType;
import common.JSONUtil;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Driver Service Server - Manages all driver connections and availability.
 * Runs on port 5001 and maintains persistent connections with drivers.
 * This is a separate distributed service from the Dispatch Server.
 */
public class DriverServiceServer {
    private static final int DRIVER_PORT = 5001;  // For driver clients
    private static final int API_PORT = 5003;      // For Dispatch Server queries
    private static final String DB_SERVICE_HOST = System.getenv("DB_SERVICE_HOST") != null ? System.getenv("DB_SERVICE_HOST") : "localhost";
    private static final int DB_SERVICE_PORT = 5002;
    
    // Thread-safe registry of active drivers
    private ConcurrentHashMap<String, DriverInfo> driverRegistry = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, DriverConnection> driverConnections = new ConcurrentHashMap<>();
    
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private volatile boolean running = true;
    
    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("DRIVER SERVICE SERVER");
        System.out.println("============================================================");
        System.out.println("[DriverService] Starting...");
        
        DriverServiceServer server = new DriverServiceServer();
        server.start();
    }
    
    public void start() {
        // Start API server for Dispatch Server queries (in separate thread)
        new Thread(this::startAPIServer).start();
        
        // Start main server for driver connections
        System.out.println("[DriverService] Listening for driver connections on port " + DRIVER_PORT);
        System.out.println("[DriverService] API server for queries on port " + API_PORT);
        System.out.println("============================================================");
        
        try (ServerSocket serverSocket = new ServerSocket(DRIVER_PORT)) {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[DriverService] New driver client connected");
                
                DriverConnection connection = new DriverConnection(clientSocket, this);
                threadPool.execute(connection);
            }
        } catch (IOException e) {
            System.err.println("[DriverService] Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }
    
    /**
     * Start API server for handling requests from Dispatch Server
     */
    private void startAPIServer() {
        try (ServerSocket apiSocket = new ServerSocket(API_PORT)) {
            System.out.println("[DriverService-API] API server ready on port " + API_PORT);
            
            while (running) {
                Socket clientSocket = apiSocket.accept();
                threadPool.execute(() -> handleAPIRequest(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[DriverService-API] API server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle API requests from Dispatch Server
     */
    private void handleAPIRequest(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            String requestJson = in.readLine();
            if (requestJson == null) return;
            
            Message request = JSONUtil.fromJSON(requestJson);
            if (request == null) return;
            
            System.out.println("[DriverService-API] Received: " + request.getType());
            
            Message response = null;
            
            switch (request.getType()) {
                case GET_AVAILABLE_DRIVERS:
                    response = getAvailableDriversList();
                    break;
                    
                case ASSIGN_DRIVER:
                    String driverUsername = request.getPayloadString("driverUsername");
                    String passengerUsername = request.getPayloadString("passengerUsername");
                    int rideId = request.getPayloadInt("rideId");
                    double pickupLat = request.getPayloadDouble("pickupLat");
                    double pickupLon = request.getPayloadDouble("pickupLon");
                    response = assignRideToDriver(driverUsername, passengerUsername, rideId, pickupLat, pickupLon);
                    break;
                    
                default:
                    response = new Message(MessageType.ERROR);
                    response.addPayload("error", "Unknown API request");
            }
            
            if (response != null) {
                String responseJson = JSONUtil.toJSON(response);
                out.println(responseJson);
            }
            
            socket.close();
        } catch (IOException e) {
            System.err.println("[DriverService-API] Error handling request: " + e.getMessage());
        }
    }
    
    /**
     * Register a new driver
     */
    public synchronized void registerDriver(String username, double lat, double lon, DriverConnection connection) {
        DriverInfo info = new DriverInfo(username);
        info.setLocation(lat, lon);
        info.setAvailable(true);
        
        driverRegistry.put(username, info);
        driverConnections.put(username, connection);
        
        // Update database
        updateDriverLocationInDB(username, lat, lon);
        
        System.out.println("[DriverService] Driver registered: " + username + " at (" + lat + ", " + lon + ")");
        System.out.println("[DriverService] Total active drivers: " + driverRegistry.size());
    }
    
    /**
     * Unregister a driver (on disconnect)
     */
    public synchronized void unregisterDriver(String username) {
        driverRegistry.remove(username);
        driverConnections.remove(username);
        System.out.println("[DriverService] Driver unregistered: " + username);
        System.out.println("[DriverService] Total active drivers: " + driverRegistry.size());
    }
    
    /**
     * Update driver location
     */
    public synchronized void updateDriverLocation(String username, double lat, double lon) {
        DriverInfo info = driverRegistry.get(username);
        if (info != null) {
            info.setLocation(lat, lon);
            updateDriverLocationInDB(username, lat, lon);
        }
    }
    
    /**
     * Update driver availability
     */
    public synchronized void updateDriverAvailability(String username, boolean available) {
        DriverInfo info = driverRegistry.get(username);
        if (info != null) {
            info.setAvailable(available);
            System.out.println("[DriverService] Driver " + username + " is now " + (available ? "AVAILABLE" : "BUSY"));
        }
    }
    
    /**
     * Get list of available drivers (for Dispatch Server)
     */
    public synchronized Message getAvailableDriversList() {
        Message response = new Message(MessageType.AVAILABLE_DRIVERS_LIST);
        
        List<Map<String, Object>> driversList = new ArrayList<>();
        
        for (DriverInfo info : driverRegistry.values()) {
            if (info.isAvailable()) {
                Map<String, Object> driverData = new HashMap<>();
                driverData.put("username", info.getUsername());
                driverData.put("latitude", info.getLatitude());
                driverData.put("longitude", info.getLongitude());
                driverData.put("available", info.isAvailable());
                driversList.add(driverData);
            }
        }
        
        // Serialize driver list to JSON string
        StringBuilder driversJson = new StringBuilder("[");
        for (int i = 0; i < driversList.size(); i++) {
            if (i > 0) driversJson.append(",");
            Map<String, Object> driver = driversList.get(i);
            driversJson.append("{");
            driversJson.append("\"username\":\"").append(driver.get("username")).append("\",");
            driversJson.append("\"latitude\":").append(driver.get("latitude")).append(",");
            driversJson.append("\"longitude\":").append(driver.get("longitude")).append(",");
            driversJson.append("\"available\":").append(driver.get("available"));
            driversJson.append("}");
        }
        driversJson.append("]");
        
        response.addPayload("drivers", driversJson.toString());
        response.addPayload("count", driversList.size());
        
        System.out.println("[DriverService] Returning " + driversList.size() + " available drivers");
        
        return response;
    }
    
    /**
     * Assign a ride to a specific driver
     */
    public synchronized Message assignRideToDriver(String driverUsername, String passengerUsername, 
                                                   int rideId, double pickupLat, double pickupLon) {
        DriverInfo info = driverRegistry.get(driverUsername);
        DriverConnection connection = driverConnections.get(driverUsername);
        
        if (info == null || connection == null) {
            Message response = new Message(MessageType.ERROR);
            response.addPayload("error", "Driver not found or not connected");
            return response;
        }
        
        if (!info.isAvailable()) {
            Message response = new Message(MessageType.ERROR);
            response.addPayload("error", "Driver is not available");
            return response;
        }
        
        // Mark driver as busy
        info.setAvailable(false);
        
        // Send ride assignment to driver
        Message assignment = new Message(MessageType.RIDE_ASSIGNMENT);
        assignment.addPayload("rideId", rideId);
        assignment.addPayload("passengerUsername", passengerUsername);
        assignment.addPayload("pickupLat", pickupLat);
        assignment.addPayload("pickupLon", pickupLon);
        
        connection.sendMessage(assignment);
        
        System.out.println("[DriverService] Assigned ride " + rideId + " to driver " + driverUsername);
        
        Message response = new Message(MessageType.DRIVER_ASSIGNED);
        response.addPayload("success", true);
        response.addPayload("driverUsername", driverUsername);
        
        return response;
    }
    
    /**
     * Notify when ride is accepted
     */
    public void notifyRideAccepted(String driverUsername, int rideId) {
        // This could notify the Dispatch Server or update database
        System.out.println("[DriverService] Ride " + rideId + " accepted by " + driverUsername);
    }
    
    /**
     * Notify when ride is started
     */
    public void notifyRideStarted(String driverUsername, int rideId, double lat, double lon) {
        // Update ride status in database
        updateRideStatusInDB(rideId, "STARTED", lat, lon);
        System.out.println("[DriverService] Ride " + rideId + " started by " + driverUsername);
    }
    
    /**
     * Notify when ride is completed
     */
    public void notifyRideCompleted(String driverUsername, int rideId, double lat, double lon) {
        // Update ride status and mark driver available
        updateRideStatusInDB(rideId, "COMPLETED", lat, lon);
        updateDriverAvailability(driverUsername, true);
        System.out.println("[DriverService] Ride " + rideId + " completed by " + driverUsername);
    }
    
    /**
     * Update driver location in database via Database Service
     */
    private void updateDriverLocationInDB(String username, double lat, double lon) {
        try {
            Socket dbSocket = new Socket(DB_SERVICE_HOST, DB_SERVICE_PORT);
            PrintWriter out = new PrintWriter(dbSocket.getOutputStream(), true);
            
            Message request = new Message(MessageType.DB_UPDATE_DRIVER_LOCATION);
            request.addPayload("username", username);
            request.addPayload("latitude", lat);
            request.addPayload("longitude", lon);
            
            String json = JSONUtil.toJSON(request);
            out.println(json);
            
            dbSocket.close();
        } catch (IOException e) {
            System.err.println("[DriverService] Error updating driver location in DB: " + e.getMessage());
        }
    }
    
    /**
     * Update ride status in database
     */
    private void updateRideStatusInDB(int rideId, String status, double lat, double lon) {
        try {
            Socket dbSocket = new Socket(DB_SERVICE_HOST, DB_SERVICE_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(dbSocket.getInputStream()));
            PrintWriter out = new PrintWriter(dbSocket.getOutputStream(), true);
            
            Message request = new Message(MessageType.DB_UPDATE_RIDE);
            request.addPayload("rideId", rideId);
            request.addPayload("status", status);
            request.addPayload("latitude", lat);
            request.addPayload("longitude", lon);
            
            String json = JSONUtil.toJSON(request);
            out.println(json);
            
            // Wait for response
            String response = in.readLine();
            
            dbSocket.close();
        } catch (IOException e) {
            System.err.println("[DriverService] Error updating ride status in DB: " + e.getMessage());
        }
    }
}
