package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private String role; // "PASSENGER" or "DRIVER"
    private DatabaseManager db;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.db = new DatabaseManager();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message = in.readLine();
            if (message == null)
                return;

            // HYBRID SERVER LOGIC: Check if it's HTTP or Valid Raw Command
            if (message.startsWith("GET") || message.startsWith("POST")) {
                handleHTTPRequest(message);
            } else {
                handleSocketCommand(message);
            }

        } catch (IOException e) {
            System.err.println("Client disconnected/error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // ==========================================
    // RAW SOCKET HANDLING (Legacy/Swing Clients)
    // ==========================================
    private void handleSocketCommand(String firstLine) throws IOException {
        out.println("WELCOME. Please login using: LOGIN <ROLE> <USERNAME>");
        // Example: LOGIN PASSENGER JOHN
        // Example: LOGIN DRIVER MIKE
        processCommand(firstLine); // Process 1st line
        String message;
        while ((message = in.readLine()) != null) {
            processCommand(message);
        }
    }

    private void processCommand(String message) {
        System.out.println("[SOCKET]: " + message);
        String[] parts = message.split(" ");
        String command = parts[0].toUpperCase();

        if (command.equals("LOGIN") && parts.length == 3) {
            role = parts[1].toUpperCase();
            username = parts[2];
            if (role.equals("PASSENGER"))
                db.registerPassenger(username, "pass", "000");
            else if (role.equals("DRIVER"))
                db.registerDriver(username, "pass", "000");

            out.println("LOGIN_SUCCESS " + role + " " + username);
            System.out.println("User logged in: " + username + " as " + role);
            RideSharingServer.activeClients.put(username, this);

        } else if (command.equals("LOC") && parts.length == 3) {
            if (username == null) {
                out.println("ERROR Please login first");
                return;
            }
            try {
                double lat = Double.parseDouble(parts[1]);
                double lon = Double.parseDouble(parts[2]);
                if (role.equals("PASSENGER"))
                    db.updatePassengerLocation(username, lat, lon);
                else if (role.equals("DRIVER"))
                    db.updateDriverLocation(username, lat, lon);
                out.println("LOCATION_UPDATED");
                System.out.println("Location updated for " + username);
            } catch (Exception e) {
                out.println("ERROR Invalid coordinates");
            }

        } else if (command.equals("REQUEST_RIDE") && parts.length == 3) {
            if (username == null || !role.equals("PASSENGER")) {
                out.println("ERROR Only logged-in passengers can request rides");
                return;
            }
            double lat = Double.parseDouble(parts[1]);
            double lon = Double.parseDouble(parts[2]);
            String driver = db.findNearestDriver(lat, lon);
            if (driver != null) {
                db.createRide(username, driver, lat, lon);
                out.println("RIDE_ASSIGNED Driver: " + driver);
                notifyDriver(driver, username, lat, lon);
            } else {
                out.println("NO_DRIVERS_AVAILABLE");
            }
        } else {
            out.println("UNKNOWN_COMMAND");
        }
    }

    // ==========================================
    // HTTP HANDLING (Web Clients)
    // ==========================================
    private void handleHTTPRequest(String requestLine) throws IOException {
        System.out.println("[HTTP]: " + requestLine);
        // We only care about the URL parameters for this simple demo
        // Format: GET /api?cmd=LOGIN&role=PASSENGER&name=John... HTTP/1.1

        String responseBody = "{\"status\":\"ok\"}";
        String status = "200 OK";

        try {
            // Parse URL params manually (Simple Parser)
            if (requestLine.contains("/api")) {
                String queryString = requestLine.split(" ")[1];
                if (queryString.contains("?")) {
                    String[] params = queryString.split("\\?")[1].split("&");
                    String cmd = null, r = null, user = null, lat = null, lon = null;

                    for (String p : params) {
                        String[] kv = p.split("=");
                        if (kv[0].equals("cmd"))
                            cmd = kv[1];
                        if (kv[0].equals("role"))
                            r = kv[1];
                        if (kv[0].equals("name"))
                            user = kv[1];
                        if (kv[0].equals("lat"))
                            lat = kv[1];
                        if (kv[0].equals("lon"))
                            lon = kv[1];
                    }

                    if ("LOGIN".equals(cmd)) {
                        // Passwords are passed as param 'pass' (in a real app, use POST body)
                        String pass = null; // Extract if available
                        for (String p : params)
                            if (p.startsWith("pass="))
                                pass = p.split("=")[1];
                        if (pass == null)
                            pass = "x"; // Fallback for old calls

                        boolean valid = db.validateLogin(r, user, pass);
                        if (valid) {
                            responseBody = "{\"status\":\"logged_in\", \"user\":\"" + user + "\"}";
                        } else {
                            responseBody = "{\"status\":\"error\", \"message\":\"Invalid credentials\"}";
                        }
                    } else if ("REGISTER".equals(cmd)) {
                        // REGISTER logic
                        String pass = null, phone = null;
                        for (String p : params)
                            if (p.startsWith("pass="))
                                pass = p.split("=")[1];
                        for (String p : params)
                            if (p.startsWith("phone="))
                                phone = p.split("=")[1];

                        boolean success = false;
                        if ("PASSENGER".equals(r))
                            success = db.registerPassenger(user, pass, phone);
                        else if ("DRIVER".equals(r))
                            success = db.registerDriver(user, pass, phone);

                        if (success)
                            responseBody = "{\"status\":\"registered\", \"user\":\"" + user + "\"}";
                        else
                            responseBody = "{\"status\":\"error\", \"message\":\"Registration failed (User exists?)\"}";
                    } else if ("LOC".equals(cmd)) {
                        Double l1 = Double.parseDouble(lat);
                        Double l2 = Double.parseDouble(lon);
                        if ("PASSENGER".equals(r))
                            db.updatePassengerLocation(user, l1, l2);
                        if ("DRIVER".equals(r))
                            db.updateDriverLocation(user, l1, l2);
                        responseBody = "{\"status\":\"updated\"}";
                    } else if ("REQUEST".equals(cmd)) {
                        Double l1 = Double.parseDouble(lat);
                        Double l2 = Double.parseDouble(lon);
                        String driver = db.findNearestDriver(l1, l2);
                        if (driver != null) {
                            db.createRide(user, driver, l1, l2);
                            notifyDriver(driver, user, l1, l2);
                            responseBody = "{\"status\":\"assigned\", \"driver\":\"" + driver + "\"}";
                        } else {
                            responseBody = "{\"status\":\"no_drivers\"}";
                        }
                    }
                }
            }
        } catch (Exception e) {
            status = "400 Bad Request";
            responseBody = "{\"error\":\"" + e.getMessage() + "\"}";
        }

        // Send HTTP Response
        out.println("HTTP/1.1 " + status);
        out.println("Access-Control-Allow-Origin: *"); // Enable CORS
        out.println("Content-Type: application/json");
        out.println("");
        out.println(responseBody);
    }

    private void notifyDriver(String driver, String passenger, double lat, double lon) {
        ClientHandler h = RideSharingServer.activeClients.get(driver);
        if (h != null) {
            h.sendMessage("NEW_RIDE Passenger: " + passenger + " Location: " + lat + "," + lon);
        } else {
            System.out.println(
                    "Driver " + driver + " assigned but not currently connected via socket.");
        }
    }

    public void sendMessage(String msg) {
        if (out != null)
            out.println(msg);
    }

    private void cleanup() {
        if (username != null)
            RideSharingServer.activeClients.remove(username);
        try {
            if (socket != null)
                socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
