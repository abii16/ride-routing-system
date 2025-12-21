package services.gateway;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import common.Message;
import common.MessageType;
import common.JSONUtil;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * WEB GATEWAY SERVER
 * Acts as a bridge between the Web Frontend (HTTP) and Distributed Backend (TCP
 * Sockets).
 * Handles:
 * 1. Serving HTML/CSS/JS files
 * 2. Geocoding (Address -> Coodinates)
 * 3. Proxying API requests to Backend Services
 */
public class WebGatewayServer {
    private static final int PORT = 8080;

    // Backend Config
    private static final String DISPATCH_HOST = System.getenv("DISPATCH_HOST") != null ? System.getenv("DISPATCH_HOST")
            : "localhost";
    private static final int DISPATCH_PORT = 5000;
    private static final String DRIVER_HOST = System.getenv("DRIVER_HOST") != null ? System.getenv("DRIVER_HOST")
            : "localhost";
    private static final int DRIVER_PORT = 5001; // Direct driver connection

    public static void main(String[] args) throws IOException {
        String myIP = getLocalIpAddress();

        System.out.println("==================================================");
        System.out.println("WEB GATEWAY SERVER (HTTP -> TCP Bridge)");
        System.out.println("==================================================");
        System.out.println("Server is listening on ALL network interfaces");
        System.out.println("");
        System.out.println("LOCAL ACCESS:");
        System.out.println("  http://localhost:" + PORT + "/");
        System.out.println("");
        System.out.println("NETWORK ACCESS (from other PCs):");
        System.out.println("  http://" + myIP + ":" + PORT + "/");
        System.out.println("");
        System.out.println("Share the network URL with other devices on your LAN!");
        System.out.println("Geocoding Service: ACTIVE (via Nominatim)");
        System.out.println("==================================================");

        // Bind to all network interfaces (0.0.0.0) to allow external access
        HttpServer server = HttpServer.create(new InetSocketAddress((InetAddress) null, PORT), 0);

        // --- Static File Handlers ---
        server.createContext("/", new StaticFileHandler("web/index.html"));
        server.createContext("/passenger", new StaticFileHandler("web/passenger.html"));
        server.createContext("/driver", new StaticFileHandler("web/driver.html"));
        server.createContext("/map", new StaticFileHandler("web/map.html"));
        server.createContext("/admin", new StaticFileHandler("web/admin.html"));
        server.createContext("/admin.html", new StaticFileHandler("web/admin.html"));
        server.createContext("/driver-register.html", new StaticFileHandler("web/driver-register.html"));
        server.createContext("/register.html", new StaticFileHandler("web/register.html"));

        // Serve CSS/JS directories
        server.createContext("/css", new DirectoryHandler("web/css"));
        server.createContext("/js", new DirectoryHandler("web/js"));

        // --- API Handlers ---
        server.createContext("/api/geocode", new GeocodeHandler());
        server.createContext("/api/passenger", new PassengerProxyHandler());
        server.createContext("/api/driver", new DriverProxyHandler());
        server.createContext("/api/mapdata", new MapDataHandler());
        server.createContext("/api/direction", new DirectionProxyHandler());

        // Auth Handlers
        server.createContext("/api/admin/register-driver", new AdminDriverHandler());
        server.createContext("/api/admin/assign-ride", new AdminAssignHandler()); // Manual Assign
        server.createContext("/api/auth/login", new AuthHandler(false)); // Login
        server.createContext("/api/passenger/register", new AuthHandler(true)); // Register

        // Detailed Driver Onboarding
        server.createContext("/api/secure/driver-registration", new DriverRegistrationHandler());
        server.createContext("/api/admin/pending-drivers", new PendingDriversHandler());
        server.createContext("/api/admin/approve-driver", new ApproveDriverHandler());

        server.setExecutor(null);
        server.start();
    }

    // --- Helper: Get LAN IP Address ---
    private static String getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }

    // --- Handler: Static Files ---
    static class StaticFileHandler implements HttpHandler {
        private String filePath;

        public StaticFileHandler(String path) {
            this.filePath = path;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            File file = new File(filePath);
            if (!file.exists()) {
                String response = "File not found: " + filePath;
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            // Set content type
            if (filePath.endsWith(".html"))
                t.getResponseHeaders().set("Content-Type", "text/html");
            if (filePath.endsWith(".css"))
                t.getResponseHeaders().set("Content-Type", "text/css");
            if (filePath.endsWith(".js"))
                t.getResponseHeaders().set("Content-Type", "application/javascript");

            byte[] bytes = Files.readAllBytes(file.toPath());
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // --- Handler: Directory (Hack for simple serving) ---
    static class DirectoryHandler implements HttpHandler {
        private String rootDir;

        public DirectoryHandler(String dir) {
            this.rootDir = dir;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            String fileName = path.substring(path.lastIndexOf("/") + 1);
            File file = new File(rootDir, fileName);

            if (!file.exists()) {
                t.sendResponseHeaders(404, -1);
                return;
            }

            if (fileName.endsWith(".css"))
                t.getResponseHeaders().set("Content-Type", "text/css");
            if (fileName.endsWith(".js"))
                t.getResponseHeaders().set("Content-Type", "application/javascript");

            byte[] bytes = Files.readAllBytes(file.toPath());
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // --- Handler: Geocoding (OpenStreetMap Nominatim) ---
    static class GeocodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // CORS
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if ("GET".equals(t.getRequestMethod())) {
                String query = t.getRequestURI().getQuery();
                String address = query.replace("q=", "");

                System.out.println("[WebGateway] Geocoding request: " + address);

                try {
                    String urlStr = "https://nominatim.openstreetmap.org/search?q=" +
                            URLEncoder.encode(address, "UTF-8") + "&format=json&limit=1";

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "DistributedSystem_UniversityProject"); // Required by OSM

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    byte[] responseBytes = content.toString().getBytes(StandardCharsets.UTF_8);
                    t.getResponseHeaders().set("Content-Type", "application/json");
                    t.sendResponseHeaders(200, responseBytes.length);
                    OutputStream os = t.getResponseBody();
                    os.write(responseBytes);
                    os.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    t.sendResponseHeaders(500, -1);
                }
            }
        }
    }

    // --- Handler: Message Proxy (HTTP -> TCP) ---
    static class PassengerProxyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            handleProxy(t, DISPATCH_HOST, DISPATCH_PORT);
        }
    }

    static class DriverProxyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // Drivers normally connect via long-lived socket.
            // For web, we'll assume stateless updates for simplicity or implement a simple
            // registration
            // In a real web app, we'd use WebSockets. Here we'll do simple HTTP->TCP
            // command firing.
            handleProxy(t, DRIVER_HOST, 5003); // Use API port for driver operations if possible
        }
    }

    private static void handleProxy(HttpExchange t, String host, int port) throws IOException {
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(t.getRequestMethod())) {
            t.sendResponseHeaders(204, -1);
            return;
        }

        if ("POST".equals(t.getRequestMethod())) {
            // Read JSON from HTTP Body
            InputStreamReader isr = new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line);
            String jsonRequest = sb.toString();

            System.out.println("[WebGateway] Proxying to " + host + ":" + port + " -> " + jsonRequest);

            // Forward to Backend via TCP Socket
            String jsonResponse = "{\"error\": \"Backend unavailable\"}";
            try (Socket socket = new Socket(host, port);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(jsonRequest);
                String resp = in.readLine();
                if (resp != null)
                    jsonResponse = resp;

            } catch (Exception e) {
                System.err.println("[WebGateway] Proxy Error: " + e.getMessage());
            }

            // Send Response back to HTTP Client
            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // --- Handler: Map Data (Get all locations) ---
    static class MapDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            // Query Database Service (Port 5002)
            String jsonResponse = "{\"drivers\":[], \"passengers\":[]}";
            try (Socket socket = new Socket("localhost", 5002);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                Message req = new Message(MessageType.GET_LOCATIONS);
                out.println(JSONUtil.toJSON(req));

                String resp = in.readLine();
                if (resp != null) {
                    // Extract payload to return simple JSON
                    Message msg = JSONUtil.fromJSON(resp);
                    // Just return the raw message payload as JSON for simplicity
                    jsonResponse = resp;
                }
            } catch (Exception e) {
                // ignore
            }

            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // --- Handler: Direction API Proxy (Gebeta) ---
    static class DirectionProxyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if ("GET".equals(t.getRequestMethod())) {
                String query = t.getRequestURI().getQuery();
                // Expected query:
                // originLat=...&originLon=...&destLat=...&destLon=...&apiKey=...
                Map<String, String> params = queryToMap(query);

                String origin = "{" + params.get("originLat") + "," + params.get("originLon") + "}";
                String dest = "{" + params.get("destLat") + "," + params.get("destLon") + "}";
                String apiKey = params.get("apiKey");

                String urlStr = "https://mapapi.gebeta.app/api/route/direction/?origin=" + origin +
                        "&destination=" + dest + "&apiKey=" + apiKey;

                System.out.println("[WebGateway] Fetching Direction: " + urlStr);

                try {

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null)
                        content.append(inputLine);
                    in.close();

                    byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);
                    t.getResponseHeaders().set("Content-Type", "application/json");
                    t.sendResponseHeaders(200, bytes.length);
                    OutputStream os = t.getResponseBody();
                    os.write(bytes);
                    os.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    t.sendResponseHeaders(500, -1);
                }
            }
        }

        private Map<String, String> queryToMap(String query) {
            Map<String, String> result = new HashMap<>();
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1)
                    result.put(entry[0], entry[1]);
            }
            return result;
        }
    }

    // --- Handler: Admin Register Driver ---
    static class AdminDriverHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if ("POST".equals(t.getRequestMethod())) {
                String body = readBody(t);
                Message msg = JSONUtil.fromJSON(body); // Expects payload with username, password

                // Construct DB Request
                Message dbReq = new Message(MessageType.DB_INSERT_DRIVER);
                dbReq.setPayload(msg.getPayload());

                String resp = sendToDatabase(dbReq);
                sendResponse(t, resp);
            } else {
                t.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- Handler: Auth (Login/Register) ---
    static class AuthHandler implements HttpHandler {
        private boolean isRegister;

        public AuthHandler(boolean isRegister) {
            this.isRegister = isRegister;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                if ("OPTIONS".equals(t.getRequestMethod())) {
                    t.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equals(t.getRequestMethod())) {
                    String body = readBody(t);
                    System.out.println("[WebGateway] Auth Request: " + body);
                    Message msg = JSONUtil.fromJSON(body);
                    if (msg == null)
                        throw new Exception("Invalid JSON request");

                    Message dbReq;
                    if (isRegister) {
                        dbReq = new Message(MessageType.DB_INSERT_PASSENGER); // Only passengers register themselves
                        dbReq.setPayload(msg.getPayload());
                    } else {
                        dbReq = new Message(MessageType.DB_VALIDATE_LOGIN);
                        dbReq.addPayload("role", msg.getPayloadString("role")); // "driver" or "passenger"
                        dbReq.addPayload("username", msg.getPayloadString("username"));
                        dbReq.addPayload("password", msg.getPayloadString("password"));
                    }

                    String resp = sendToDatabase(dbReq);
                    sendResponse(t, resp);
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(t, "{\"success\":false, \"error\":\"Auth Gateway Error: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- Handler: Detailed Driver Onboarding ---
    static class DriverRegistrationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                if ("OPTIONS".equals(t.getRequestMethod())) {
                    t.sendResponseHeaders(204, -1);
                    return;
                }
                if ("POST".equals(t.getRequestMethod())) {
                    String body = readBody(t);
                    System.out.println("[WebGateway] Driver Registration Request: " + body);

                    // File Logging for diagnostics
                    try (FileWriter fw = new FileWriter("gateway_log.txt", true)) {
                        fw.write("\n--- [" + new java.util.Date() + "] REGISTRATION REQUEST ---\n");
                        fw.write(body);
                        fw.write("\n----------------------------------------------------\n");
                    } catch (Exception logEx) {
                    }

                    Message msg = JSONUtil.fromJSON(body);
                    if (msg == null)
                        throw new Exception("Invalid JSON request");

                    Message dbReq = new Message(MessageType.DB_INSERT_DRIVER_DETAILED);
                    dbReq.setPayload(msg.getPayload());
                    sendResponse(t, sendToDatabase(dbReq));
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(t,
                        "{\"success\":false, \"error\":\"Registration Gateway Error: " + e.getMessage() + "\"}");
            }
        }
    }

    static class PendingDriversHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            if ("OPTIONS".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }
            try {
                if ("GET".equals(t.getRequestMethod())) {
                    Message dbReq = new Message(MessageType.DB_GET_PENDING_DRIVERS);
                    sendResponse(t, sendToDatabase(dbReq));
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(t, "{\"success\":false, \"error\":\"Gateway Error: " + e.getMessage() + "\"}");
            }
        }
    }

    static class ApproveDriverHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }
            try {
                if ("POST".equals(t.getRequestMethod())) {
                    String body = readBody(t);
                    System.out.println("[WebGateway] Approve Request: " + body);
                    Message msg = JSONUtil.fromJSON(body);
                    if (msg == null)
                        throw new Exception("Invalid JSON Request");

                    Message dbReq = new Message(MessageType.DB_APPROVE_DRIVER);
                    dbReq.setPayload(msg.getPayload());
                    sendResponse(t, sendToDatabase(dbReq));
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(t, "{\"success\":false, \"error\":\"Gateway Error: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- Handler: Admin Assign Driver ---
    static class AdminAssignHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(t.getRequestMethod())) {
                String body = readBody(t);
                Message msg = JSONUtil.fromJSON(body);
                Map<String, Object> payload = msg.getPayload();

                // Construct Dispatch Request
                Message dMsg = new Message(MessageType.ASSIGN_DRIVER); // Needs ASSIGN_DRIVER support in Dispatch
                dMsg.setPayload(payload);

                // Manual Fix: PassengerHandler doesn't support ASSIGN_DRIVER yet (it treats as
                // client).
                // But we can add support in DispatchServer side.

                String jsonResponse = "{\"success\": false, \"error\": \"Dispatch Unavailable\"}";
                try (Socket socket = new Socket(DISPATCH_HOST, DISPATCH_PORT);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println(JSONUtil.toJSON(dMsg));
                    String resp = in.readLine();
                    if (resp != null) {
                        Message m = JSONUtil.fromJSON(resp);
                        // If we get an error or success message
                        if (m.getType() == MessageType.RIDE_ASSIGNMENT) {
                            jsonResponse = mapToJson(m.getPayload());
                        } else {
                            jsonResponse = mapToJson(m.getPayload());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                sendResponse(t, jsonResponse);
            }
        }
    }

    // --- Helper Methods ---
    private static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first)
                sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String)
                sb.append("\"").append(val).append("\"");
            else
                sb.append(val);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String readBody(HttpExchange t) throws IOException {
        InputStreamReader isr = new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line);
        return sb.toString();
    }

    private static String sendToDatabase(Message msg) {
        String jsonReq = JSONUtil.toJSON(msg);
        System.out.println("[WebGateway] Sending to DB: "
                + (jsonReq.length() > 100 ? jsonReq.substring(0, 100) + "..." : jsonReq));
        try (Socket socket = new Socket("localhost", 5002);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(jsonReq);
            String resp = in.readLine();
            if (resp == null) {
                System.err.println("[WebGateway] DB Service returned NULL (Connection closed?)");
                return "{\"success\":false, \"error\":\"Database connection reset\"}";
            }
            return resp;
        } catch (Exception e) {
            System.err.println("[WebGateway] DB Service unavailable: " + e.getMessage());
            return "{\"success\":false, \"error\":\"Database unavailable: " + e.getMessage() + "\"}";
        }
    }

    private static void sendResponse(HttpExchange t, String json) throws IOException {
        if (json == null)
            json = "{\"success\":false, \"error\":\"Internal Server Error (Empty Response)\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(bytes);
        } catch (IOException e) {
            System.err.println("[WebGateway] Failed to send HTTP response: " + e.getMessage());
            throw e;
        }
    }
}
