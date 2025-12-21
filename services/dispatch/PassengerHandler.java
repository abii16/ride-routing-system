package services.dispatch;

import common.Message;
import common.MessageType;
import common.JSONUtil;

import java.io.*;
import java.net.*;

/**
 * Passenger Handler - Handles individual passenger connections in the Dispatch
 * Server
 */
public class PassengerHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private volatile boolean running = true;

    private static final String DB_SERVICE_HOST = "localhost";
    private static final int DB_SERVICE_PORT = 5002;

    public PassengerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("[PassengerHandler] New passenger connection");

            // Handle messages from passenger
            String line;
            while (running && (line = in.readLine()) != null) {
                handleMessage(line);
            }

        } catch (IOException e) {
            System.err.println("[PassengerHandler] Connection error: " + e.getMessage());
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

            System.out.println("[PassengerHandler] Received " + message.getType() +
                    (username != null ? " from " + username : ""));

            switch (message.getType()) {
                case REGISTER_PASSENGER:
                    handleRegister(message);
                    break;

                case LOGIN:
                    handleLogin(message);
                    break;

                case UPDATE_LOCATION:
                    handleLocationUpdate(message);
                    break;

                case RIDE_REQUEST:
                    handleRideRequest(message);
                    break;

                case ASSIGN_DRIVER:
                    handleManualAssign(message); // New Method
                    break;

                case RIDE_CANCELLED:
                    handleRideCancellation(message);
                    break;

                case DISCONNECT:
                    running = false;
                    break;

                default:
                    sendError("Unknown message type: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("[PassengerHandler] Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRegister(Message message) {
        String username = message.getPayloadString("username");
        String password = message.getPayloadString("password");
        String phone = message.getPayloadString("phone");

        // Send registration request to Database Service
        Message dbRequest = new Message(MessageType.DB_INSERT_PASSENGER);
        dbRequest.addPayload("username", username);
        dbRequest.addPayload("password", password);
        dbRequest.addPayload("phone", phone);

        Message dbResponse = sendToDatabaseService(dbRequest);

        if (dbResponse != null && dbResponse.getPayloadBoolean("success")) {
            this.username = username;
            DispatchServer.registerPassenger(username, this);

            Message response = new Message(MessageType.LOGIN_SUCCESS);
            response.addPayload("username", username);
            response.addPayload("message", "Registration successful");
            sendMessage(response);

            System.out.println("[PassengerHandler] Passenger registered: " + username);
        } else {
            Message response = new Message(MessageType.LOGIN_FAILED);
            response.addPayload("error",
                    dbResponse != null ? dbResponse.getPayloadString("error") : "Registration failed");
            sendMessage(response);
        }
    }

    private void handleLogin(Message message) {
        String username = message.getPayloadString("username");
        String password = message.getPayloadString("password");

        // Validate login via Database Service
        Message dbRequest = new Message(MessageType.DB_VALIDATE_LOGIN);
        dbRequest.addPayload("role", "PASSENGER");
        dbRequest.addPayload("username", username);
        dbRequest.addPayload("password", password);

        Message dbResponse = sendToDatabaseService(dbRequest);

        if (dbResponse != null && dbResponse.getPayloadBoolean("valid")) {
            this.username = username;
            DispatchServer.registerPassenger(username, this);

            Message response = new Message(MessageType.LOGIN_SUCCESS);
            response.addPayload("username", username);
            response.addPayload("message", "Login successful");
            sendMessage(response);

            System.out.println("[PassengerHandler] Passenger logged in: " + username);
        } else {
            Message response = new Message(MessageType.LOGIN_FAILED);
            response.addPayload("error", "Invalid credentials");
            sendMessage(response);
        }
    }

    private void handleLocationUpdate(Message message) {
        String effectiveUsername = this.username;
        if (effectiveUsername == null) {
            effectiveUsername = message.getPayloadString("username");
        }

        if (effectiveUsername == null) {
            sendError("Please login first or provide username in payload");
            return;
        }

        double lat = message.getPayloadDouble("latitude");
        double lon = message.getPayloadDouble("longitude");

        // Update location in Database Service
        Message dbRequest = new Message(MessageType.DB_UPDATE_PASSENGER_LOCATION);
        dbRequest.addPayload("username", effectiveUsername);
        dbRequest.addPayload("latitude", lat);
        dbRequest.addPayload("longitude", lon);

        sendToDatabaseService(dbRequest);

        Message response = new Message(MessageType.LOCATION_UPDATED);
        sendMessage(response);
    }

    private void handleRideRequest(Message message) {
        String effectiveUsername = this.username;
        if (effectiveUsername == null) {
            effectiveUsername = message.getPayloadString("username");
        }

        if (effectiveUsername == null) {
            sendError("Please login first or provide username in payload");
            return;
        }

        Double pickupLat = message.getPayloadDouble("pickupLat");
        Double pickupLon = message.getPayloadDouble("pickupLon");
        Double destLat = message.getPayloadDouble("destLat");
        Double destLon = message.getPayloadDouble("destLon");
        String startAddr = message.getPayloadString("pickupAddr");
        String destAddr = message.getPayloadString("destAddr");

        if (pickupLat == null || pickupLon == null || destLat == null || destLon == null) {
            sendError("Incomplete location data. Please select pickup and destination on the map.");
            return;
        }

        System.out.println("[PassengerHandler] Processing ride request for " + effectiveUsername);
        System.out.println("  Pickup: " + startAddr + " (" + pickupLat + ", " + pickupLon + ")");
        System.out.println("  Destination: " + destAddr + " (" + destLat + ", " + destLon + ")");

        // Assign nearest driver (this uses synchronized driver assignment)
        String result = DispatchServer.assignNearestDriver(effectiveUsername, pickupLat, pickupLon, destLat, destLon,
                startAddr,
                destAddr);

        if (result != null) {
            String[] parts = result.split(":");

            if (parts[0].equals("WAITING")) {
                int rideId = Integer.parseInt(parts[1]);

                // Send "Waiting for Driver" response (Using RIDE_ASSIGNMENT message but with
                // status)
                // Actually, NO_DRIVERS_AVAILABLE might be misinterpreted as error.
                // Let's send a new TYPE or just RIDE_ASSIGNMENT with null driver?
                // Easier: Use NO_DRIVERS_AVAILABLE but include rideId so client knows it is
                // QUEUED.

                Message response = new Message(MessageType.RIDE_REQUEST); // Echo back request as pending?
                // Or better: Use a new status or just standard NO_DRIVERS_AVAILABLE but
                // success=true?

                // Let's use NO_DRIVERS_AVAILABLE for now, but update client text.
                response = new Message(MessageType.NO_DRIVERS_AVAILABLE);
                response.addPayload("message", "Request received. Searching for drivers... (Ride #" + rideId + ")");
                response.addPayload("rideId", rideId);
                sendMessage(response);

                System.out.println("[PassengerHandler] Ride #" + rideId + " queued (No drivers)");

            } else {
                String driverUsername = parts[0];
                int rideId = Integer.parseInt(parts[1]);

                Message response = new Message(MessageType.RIDE_ASSIGNMENT);
                response.addPayload("success", true);
                response.addPayload("rideId", rideId);
                response.addPayload("driverUsername", driverUsername);
                response.addPayload("message", "Driver assigned successfully");
                sendMessage(response);

                System.out.println("[PassengerHandler] Ride assigned: " + driverUsername + " to " + effectiveUsername);
            }
        } else {
            // Failed to even create ride in DB
            Message response = new Message(MessageType.ERROR);
            response.addPayload("error", "Failed to process ride request");
            sendMessage(response);

            System.out.println("[PassengerHandler] Failed to create ride for " + effectiveUsername);
        }
    }

    private void handleManualAssign(Message message) {
        int rideId = message.getPayloadInt("rideId");
        String driver = message.getPayloadString("driverUsername");
        double pLat = message.getPayloadDouble("pickupLat");
        double pLon = message.getPayloadDouble("pickupLon");

        System.out.println("[PassengerHandler] Manual Assignment: Ride " + rideId + " -> " + driver);

        boolean success = DispatchServer.forceAssignDriver(rideId, driver, pLat, pLon);

        Message response = new Message(MessageType.RIDE_ASSIGNMENT);
        response.addPayload("success", success);
        if (!success)
            response.addPayload("error", "Assignment failed (Driver might be missing or busy)");
        sendMessage(response);
    }

    private void handleRideCancellation(Message message) {
        int rideId = message.getPayloadInt("rideId");
        String driverUsername = message.getPayloadString("driverUsername");

        // Release driver
        if (driverUsername != null) {
            DispatchServer.releaseDriver(driverUsername);
        }

        System.out.println("[PassengerHandler] Ride cancelled: " + rideId);
    }

    /**
     * Send message to Database Service and wait for response
     */
    private Message sendToDatabaseService(Message request) {
        try {
            Socket dbSocket = new Socket(DB_SERVICE_HOST, DB_SERVICE_PORT);
            BufferedReader dbIn = new BufferedReader(new InputStreamReader(dbSocket.getInputStream()));
            PrintWriter dbOut = new PrintWriter(dbSocket.getOutputStream(), true);

            String requestJson = JSONUtil.toJSON(request);
            dbOut.println(requestJson);

            String responseJson = dbIn.readLine();
            dbSocket.close();

            if (responseJson != null) {
                return JSONUtil.fromJSON(responseJson);
            }
        } catch (IOException e) {
            System.err.println("[PassengerHandler] Error communicating with Database Service: " + e.getMessage());
        }

        return null;
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
        if (username != null) {
            DispatchServer.unregisterPassenger(username);
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
