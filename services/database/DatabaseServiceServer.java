package services.database;

import common.Message;
import common.MessageType;
import common.JSONUtil;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Database Service Server - Isolated process for database operations.
 * Runs on port 5002 and handles all database requests via sockets.
 * This demonstrates true distributed architecture by separating database logic.
 */
public class DatabaseServiceServer {
    private static final int PORT = 5002;
    private static DatabaseManager dbManager;
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    private static volatile boolean running = true;

    // List of other DB Services to sync with
    private static final java.util.List<String> SYNC_HOSTS = new java.util.ArrayList<>();

    public static void main(String[] args) {
        String syncConfig = null;

        // 1. Try reading from config.properties
        try (InputStream input = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            syncConfig = prop.getProperty("SYNC_DB_HOSTS");
        } catch (IOException ex) {
            // Ignore, file might not exist
        }

        // 2. Fallback to Env Var
        if (syncConfig == null || syncConfig.isEmpty()) {
            syncConfig = System.getenv("SYNC_DB_HOSTS");
        }

        // Read Sync Hosts from Config or Env
        if (syncConfig != null && !syncConfig.isEmpty()) {
            for (String host : syncConfig.split(",")) {
                SYNC_HOSTS.add(host.trim());
            }
            System.out.println("[DatabaseService] Synchronization Enabled with: " + SYNC_HOSTS);
        }

        System.out.println("============================================================");
        System.out.println("DATABASE SERVICE SERVER");
        System.out.println("============================================================");
        System.out.println("[DatabaseService] Starting on port " + PORT);

        // Initialize database connection
        dbManager = new DatabaseManager();

        // --- AUTO-DISCOVERY STARTUP ---
        // Start listening and broadcasting presence
        new Thread(new DiscoveryService()).start();

        // --- MANUAL/CONFIG SYNC ---
        if (!SYNC_HOSTS.isEmpty()) {
            performInitialSync();
        }

        // Start server
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[DatabaseService] Ready to accept connections");
            System.out.println("[DatabaseService] Waiting for requests from Dispatch Server and Driver Service...");
            System.out.println("============================================================");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[DatabaseService] New connection from: " + clientSocket.getInetAddress());

                // Handle each request in a separate thread
                threadPool.execute(new DatabaseRequestHandler(clientSocket, dbManager, SYNC_HOSTS));
            }
        } catch (IOException e) {
            System.err.println("[DatabaseService] Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbManager.close();
            threadPool.shutdown();
        }
    }

    // Called by DiscoveryService when a new peer is found

    public static void shutdown() {
        running = false;
    }

    private static void performInitialSync() {
        if (SYNC_HOSTS.isEmpty())
            return;
        System.out.println("[DatabaseService] Performing Initial Sync with configured hosts...");
        for (String host : SYNC_HOSTS) {
            performSingleSync(host);
        }
    }

    // Called by DiscoveryService when a new peer is found
    public static synchronized void addSyncPeer(String ip) {
        if (!SYNC_HOSTS.contains(ip)) {
            System.out.println("[AutoDiscovery] Found new peer: " + ip + ". Added to Sync List.");
            SYNC_HOSTS.add(ip);
            // Trigger an initial sync with this new peer immediately
            new Thread(() -> performSingleSync(ip)).start();
        }
    }

    private static void performSingleSync(String host) {
        try (Socket s = new Socket(host, 5002);
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            System.out.println("[DatabaseService] Requesting data from found peer " + host);

            Message req = new Message(MessageType.SYNC_REQUEST);
            out.println(JSONUtil.toJSON(req));

            String resJson = in.readLine();
            if (resJson != null) {
                Message res = JSONUtil.fromJSON(resJson);
                if (res != null && res.getType() == MessageType.SYNC_DATA_RESPONSE) {
                    System.out.println("[DatabaseService] Received data. Importing...");
                    dbManager.importData(res.getPayloadString("passengers"), res.getPayloadString("drivers"));
                    System.out.println("[DatabaseService] Auto-Sync Complete with " + host);
                }
            }
        } catch (Exception e) {
            System.err.println("[DatabaseService] Auto-Sync failed with " + host + ": " + e.getMessage());
        }
    }

    /**
     * Auto-Discovery Service using UDP Broadcast
     */
    static class DiscoveryService implements Runnable {
        private static final int DISCOVERY_PORT = 8888;
        private static final String DISCOVERY_MSG = "DISRIBUTED_RIDE_SHARE_DISCOVERY";

        @Override
        public void run() {
            System.out.println("[AutoDiscovery] Started. Looking for peers on LAN...");
            new Thread(this::listenForPeers).start();
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                byte[] buffer = DISCOVERY_MSG.getBytes();
                while (true) {
                    try {
                        InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddr,
                                DISCOVERY_PORT);
                        socket.send(packet);
                        Thread.sleep(5000);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                System.err.println("[AutoDiscovery] Broadcaster failed: " + e.getMessage());
            }
        }

        private void listenForPeers() {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    String remoteIP = packet.getAddress().getHostAddress();
                    if (message.equals(DISCOVERY_MSG) && !remoteIP.equals(getLocalIpAddress())) {
                        DatabaseServiceServer.addSyncPeer(remoteIP);
                    }
                }
            } catch (Exception e) {
                System.err.println("[AutoDiscovery] Listener failed: " + e.getMessage());
            }
        }

        private String getLocalIpAddress() {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                return "";
            }
        }
    }
}

/**
 * Handles individual database requests from other services
 */
class DatabaseRequestHandler implements Runnable {
    private Socket socket;
    private DatabaseManager dbManager;
    private BufferedReader in;
    private PrintWriter out;
    private java.util.List<String> syncHosts;

    public DatabaseRequestHandler(Socket socket, DatabaseManager dbManager, java.util.List<String> syncHosts) {
        this.socket = socket;
        this.dbManager = dbManager;
        this.syncHosts = syncHosts;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Read request
            String requestJson = in.readLine();
            if (requestJson == null || requestJson.trim().isEmpty()) {
                System.err.println("[DatabaseRequestHandler] Received empty request");
                return;
            }

            System.out.println("[DatabaseRequestHandler] Received: "
                    + requestJson.substring(0, Math.min(requestJson.length(), 100)) + "...");

            // Parse request
            Message request = JSONUtil.fromJSON(requestJson);
            if (request == null) {
                sendErrorResponse("Invalid JSON format");
                return;
            }

            // Process request based on type
            Message response = processRequest(request);

            // --- SYNCHRONIZATION ---
            // If modification was successful AND type is modification AND not already a
            // sync message
            if (response != null && response.getPayloadBoolean("success") &&
                    isModificationType(request.getType()) && !request.getPayload().containsKey("isSync")) {

                syncToPeers(request);
            }

            // Send response
            String responseJson = JSONUtil.toJSON(response);
            out.println(responseJson);
            System.out.println("[DatabaseRequestHandler] Sent response for request: " + request.getRequestId());

        } catch (IOException e) {
            System.err.println("[DatabaseRequestHandler] Error handling request: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Process database request and return response
     */
    private Message processRequest(Message request) {
        MessageType type = request.getType();
        System.out.println("[DatabaseRequestHandler] Processing request type: " + type);

        try {
            switch (type) {
                case DB_INSERT_PASSENGER:
                    return dbManager.registerPassenger(
                            request.getPayloadString("username"),
                            request.getPayloadString("password"),
                            request.getPayloadString("phone"));

                case DB_INSERT_DRIVER:
                    return dbManager.registerDriver(
                            request.getPayloadString("username"),
                            request.getPayloadString("password"),
                            request.getPayloadString("phone"));

                case DB_VALIDATE_LOGIN:
                    return dbManager.validateLogin(
                            request.getPayloadString("role"),
                            request.getPayloadString("username"),
                            request.getPayloadString("password"));

                case DB_UPDATE_PASSENGER_LOCATION:
                    return dbManager.updatePassengerLocation(
                            request.getPayloadString("username"),
                            request.getPayloadDouble("latitude"),
                            request.getPayloadDouble("longitude"));

                case DB_UPDATE_DRIVER_LOCATION:
                    return dbManager.updateDriverLocation(
                            request.getPayloadString("username"),
                            request.getPayloadDouble("latitude"),
                            request.getPayloadDouble("longitude"));

                case DB_CREATE_RIDE:
                    return dbManager.createRide(
                            request.getPayloadString("passengerUsername"),
                            request.getPayloadString("driverUsername"),
                            request.getPayloadDouble("startLat"),
                            request.getPayloadDouble("startLon"),
                            request.getPayloadDouble("destLat"),
                            request.getPayloadDouble("destLon"),
                            request.getPayloadString("startAddr"),
                            request.getPayloadString("destAddr"));

                case DB_UPDATE_RIDE:
                    return dbManager.updateRideStatus(
                            request.getPayloadInt("rideId"),
                            request.getPayloadString("status"),
                            request.getPayloadDouble("latitude"),
                            request.getPayloadDouble("longitude"));

                case ASSIGN_DRIVER:
                    return dbManager.assignRideDriver(
                            request.getPayloadInt("rideId"),
                            request.getPayloadString("driverUsername"));

                case DB_GET_ACTIVE_RIDES:
                    return dbManager.getActiveRides();

                case GET_LOCATIONS:
                    return dbManager.getAllLocations();

                case DB_INSERT_DRIVER_DETAILED:
                    Map<String, Object> data = request.getPayload();
                    return dbManager.registerDriverDetailed(data);

                case DB_GET_PENDING_DRIVERS:
                    return dbManager.getPendingDrivers();

                case DB_APPROVE_DRIVER:
                    Boolean approveVal = request.getPayloadBoolean("approve");
                    return dbManager.approveDriver(
                            request.getPayloadString("username"),
                            approveVal != null ? approveVal : false);

                case SYNC_REQUEST:
                    return dbManager.getAllTableData();

                default:
                    return createErrorResponse("Unknown request type: " + type);
            }
        } catch (Exception e) {
            System.err.println("[DatabaseRequestHandler] Error processing request: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Error processing request: " + e.getMessage());
        }
    }

    private void sendErrorResponse(String errorMsg) {
        Message errorResponse = new Message(MessageType.ERROR);
        errorResponse.addPayload("error", errorMsg);
        String json = JSONUtil.toJSON(errorResponse);
        out.println(json);
    }

    private Message createErrorResponse(String errorMsg) {
        Message response = new Message(MessageType.DB_RESPONSE);
        response.addPayload("success", false);
        response.addPayload("error", errorMsg);
        return response;
    }

    // Check if request modifies data
    private boolean isModificationType(MessageType type) {
        return type == MessageType.DB_INSERT_PASSENGER ||
                type == MessageType.DB_INSERT_DRIVER ||
                type == MessageType.DB_INSERT_DRIVER_DETAILED ||
                type == MessageType.DB_APPROVE_DRIVER ||
                type == MessageType.DB_CREATE_RIDE ||
                type == MessageType.DB_UPDATE_RIDE ||
                type == MessageType.ASSIGN_DRIVER;

    }

    private void syncToPeers(Message originalRequest) {
        if (syncHosts == null || syncHosts.isEmpty())
            return;

        // Mark as sync message to prevent loops
        originalRequest.addPayload("isSync", true);
        String json = JSONUtil.toJSON(originalRequest);

        for (String host : syncHosts) {
            new Thread(() -> { // Async fire-and-forget
                try (Socket s = new Socket(host, 5002);
                        PrintWriter pw = new PrintWriter(s.getOutputStream(), true)) {
                    pw.println(json);
                    System.out.println("[DatabaseService] Synced " + originalRequest.getType() + " to " + host);
                } catch (Exception e) {
                    System.err.println("[DatabaseService] Failed to sync to " + host + ": " + e.getMessage());
                }
            }).start();
        }
    }
}
