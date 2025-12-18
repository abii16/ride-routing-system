package services.dispatch;

import common.Message;
import common.MessageType;
import common.JSONUtil;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dispatch Server - Handles passenger ride requests and assigns drivers.
 * Runs on port 5000 and coordinates with Driver Service and Database Service.
 * This is the main orchestrator of the ride-sharing system.
 */
public class DispatchServer {
    private static final int PORT = 5000;
    private static final String DRIVER_SERVICE_HOST = System.getenv("DRIVER_SERVICE_HOST") != null ? System.getenv("DRIVER_SERVICE_HOST") : "localhost";
    private static final int DRIVER_SERVICE_PORT = 5003;  // API port
    private static final String DB_SERVICE_HOST = System.getenv("DB_SERVICE_HOST") != null ? System.getenv("DB_SERVICE_HOST") : "localhost";
    private static final int DB_SERVICE_PORT = 5002;
    
    // Synchronization for driver assignment (prevents race conditions)
    private static final ReentrantLock assignmentLock = new ReentrantLock();
    private static Set<String> busyDrivers = ConcurrentHashMap.newKeySet();
    
    // Active passenger connections
    private static ConcurrentHashMap<String, PassengerHandler> activePassengers = new ConcurrentHashMap<>();
    
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    private static volatile boolean running = true;
    
    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("DISPATCH SERVER");
        System.out.println("============================================================");
        System.out.println("[DispatchServer] Starting on port " + PORT);
        System.out.println("[DispatchServer] Will connect to:");
        System.out.println("  - Driver Service: " + DRIVER_SERVICE_HOST + ":" + DRIVER_SERVICE_PORT);
        System.out.println("  - Database Service: " + DB_SERVICE_HOST + ":" + DB_SERVICE_PORT);
        System.out.println("============================================================");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[DispatchServer] Ready to accept passenger connections");
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[DispatchServer] New passenger connection from: " + clientSocket.getInetAddress());
                
                PassengerHandler handler = new PassengerHandler(clientSocket);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("[DispatchServer] Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }
    
    /**
     * Request available drivers from Driver Service
     */
    public static List<Map<String, Object>> getAvailableDrivers() {
        try {
            Socket socket = new Socket(DRIVER_SERVICE_HOST, DRIVER_SERVICE_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            Message request = new Message(MessageType.GET_AVAILABLE_DRIVERS);
            String requestJson = JSONUtil.toJSON(request);
            out.println(requestJson);
            
            String responseJson = in.readLine();
            socket.close();
            
            if (responseJson != null) {
                Message response = JSONUtil.fromJSON(responseJson);
                if (response != null && response.getType() == MessageType.AVAILABLE_DRIVERS_LIST) {
                    String driversJsonStr = response.getPayloadString("drivers");
                    return parseDriversJSON(driversJsonStr);
                }
            }
        } catch (IOException e) {
            System.err.println("[DispatchServer] Error getting available drivers: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Parse drivers JSON string to list
     */
    private static List<Map<String, Object>> parseDriversJSON(String jsonStr) {
        List<Map<String, Object>> drivers = new ArrayList<>();
        
        try {
            // Simple JSON array parser
            jsonStr = jsonStr.trim();
            if (jsonStr.startsWith("[")) jsonStr = jsonStr.substring(1);
            if (jsonStr.endsWith("]")) jsonStr = jsonStr.substring(0, jsonStr.length() - 1);
            
            if (jsonStr.trim().isEmpty()) return drivers;
            
            // Split by objects
            String[] driverObjects = jsonStr.split("\\},\\{");
            
            for (String driverObj : driverObjects) {
                driverObj = driverObj.replace("{", "").replace("}", "");
                String[] fields = driverObj.split(",");
                
                Map<String, Object> driver = new HashMap<>();
                for (String field : fields) {
                    String[] kv = field.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        
                        if (key.equals("latitude") || key.equals("longitude")) {
                            driver.put(key, Double.parseDouble(value));
                        } else if (key.equals("available")) {
                            driver.put(key, Boolean.parseBoolean(value));
                        } else {
                            driver.put(key, value);
                        }
                    }
                }
                
                drivers.add(driver);
            }
        } catch (Exception e) {
            System.err.println("[DispatchServer] Error parsing drivers JSON: " + e.getMessage());
        }
        
        return drivers;
    }
    
    /**
     * Find nearest available driver (with synchronization to prevent double-booking)
     */
    public static String assignNearestDriver(String passengerUsername, double passengerLat, double passengerLon, 
                                            double destLat, double destLon, String startAddr, String destAddr) {
        assignmentLock.lock();
        try {
            System.out.println("[DispatchServer] Assigning driver for " + passengerUsername);
            
            // Get available drivers
            List<Map<String, Object>> drivers = getAvailableDrivers();
            drivers.removeIf(d -> busyDrivers.contains(d.get("username")));
            
            // Find nearest
            Map<String, Object> nearestDriver = null;
            double minDistance = Double.MAX_VALUE;
            
            for (Map<String, Object> driver : drivers) {
                double dLat = (Double) driver.get("latitude");
                double dLon = (Double) driver.get("longitude");
                double distance = calculateDistance(passengerLat, passengerLon, dLat, dLon);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestDriver = driver;
                }
            }
            
            String driverUsername = null;
            if (nearestDriver != null) {
                driverUsername = (String) nearestDriver.get("username");
                busyDrivers.add(driverUsername); // Reserve driver
            }
            
            // ALWAYS Create ride in database (REQUESTED or ASSIGNED)
            int rideId = createRideInDB(passengerUsername, driverUsername, passengerLat, passengerLon, destLat, destLon, startAddr, destAddr);
            
            if (rideId > 0) {
                if (driverUsername != null) {
                    // Notify Driver Service
                    assignDriverViaService(driverUsername, passengerUsername, rideId, passengerLat, passengerLon);
                    return driverUsername + ":" + rideId;
                } else {
                    // No driver found, but Request logged
                    return "WAITING:" + rideId;
                }
            }
            
            // If failed, cleanup
            if (driverUsername != null) busyDrivers.remove(driverUsername);
            return null;
            
        } finally {
            assignmentLock.unlock();
        }
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Earth radius in km
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Create ride in database via Database Service
     */
    private static int createRideInDB(String passenger, String driver, double startLat, double startLon, 
                                     double destLat, double destLon, String startAddr, String destAddr) {
        try {
            Socket socket = new Socket(DB_SERVICE_HOST, DB_SERVICE_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            Message request = new Message(MessageType.DB_CREATE_RIDE);
            request.addPayload("passengerUsername", passenger);
            request.addPayload("driverUsername", driver);
            request.addPayload("startLat", startLat);
            request.addPayload("startLon", startLon);
            request.addPayload("destLat", destLat);
            request.addPayload("destLon", destLon);
            request.addPayload("startAddr", startAddr);
            request.addPayload("destAddr", destAddr);
            
            String requestJson = JSONUtil.toJSON(request);
            out.println(requestJson);
            
            String responseJson = in.readLine();
            socket.close();
            
            if (responseJson != null) {
                Message response = JSONUtil.fromJSON(responseJson);
                if (response != null && response.getPayloadBoolean("success")) {
                    return response.getPayloadInt("rideId");
                }
            }
        } catch (IOException e) {
            System.err.println("[DispatchServer] Error creating ride in DB: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Assign driver via Driver Service
     */
    private static void assignDriverViaService(String driverUsername, String passengerUsername, 
                                              int rideId, double pickupLat, double pickupLon) {
        try {
            Socket socket = new Socket(DRIVER_SERVICE_HOST, DRIVER_SERVICE_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            Message request = new Message(MessageType.ASSIGN_DRIVER);
            request.addPayload("driverUsername", driverUsername);
            request.addPayload("passengerUsername", passengerUsername);
            request.addPayload("rideId", rideId);
            request.addPayload("pickupLat", pickupLat);
            request.addPayload("pickupLon", pickupLon);
            
            String requestJson = JSONUtil.toJSON(request);
            out.println(requestJson);
            
            String responseJson = in.readLine();
            socket.close();
            
            System.out.println("[DispatchServer] Driver assignment sent to Driver Service");
        } catch (IOException e) {
            System.err.println("[DispatchServer] Error assigning driver via service: " + e.getMessage());
        }
    }
    
    /**
     * Release driver from busy set (when ride completes)
     */
    public static void releaseDriver(String driverUsername) {
        busyDrivers.remove(driverUsername);
        System.out.println("[DispatchServer] Released driver: " + driverUsername);
    }
    
    /**
     * Register passenger handler
     */
    public static void registerPassenger(String username, PassengerHandler handler) {
        activePassengers.put(username, handler);
        System.out.println("[DispatchServer] Passenger registered: " + username);
        System.out.println("[DispatchServer] Total active passengers: " + activePassengers.size());
    }
    
    /**
     * Unregister passenger handler
     */
    public static void unregisterPassenger(String username) {
        activePassengers.remove(username);
        System.out.println("[DispatchServer] Passenger unregistered: " + username);
        System.out.println("[DispatchServer] Total active passengers: " + activePassengers.size());
    }
    
    /**
     * Force assign driver (called by Admin Manual Assign)
     */
    public static boolean forceAssignDriver(int rideId, String driverUsername, double lat, double lon) {
        // 1. Mark driver as busy locally
        busyDrivers.add(driverUsername);
        
        System.out.println("[DispatchServer] FORCE ASSIGN: " + driverUsername + " to Ride " + rideId);
        
        // 2. Update DB (ASSIGN_DRIVER)
        try {
            Socket socket = new Socket(DB_SERVICE_HOST, DB_SERVICE_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            Message req = new Message(MessageType.ASSIGN_DRIVER);
            req.addPayload("rideId", rideId);
            req.addPayload("driverUsername", driverUsername);
            out.println(JSONUtil.toJSON(req));
            
            String responseStr = in.readLine();
            socket.close();
            
            Message res = JSONUtil.fromJSON(responseStr);
            if (res == null || !res.getPayloadBoolean("success")) {
                System.err.println("[DispatchServer] Failed to update DB for manual assignment");
                return false;
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        
        // 3. Notify Driver Service
        assignDriverViaService(driverUsername, "AdminManual", rideId, lat, lon);
        
        return true;
    }
}
