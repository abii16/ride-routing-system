package server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class RideSharingServer {
    private static final int PORT = 5000;
    private static ExecutorService pool = Executors.newCachedThreadPool();

    // Map to store active client handlers: Username -> Handler
    public static ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("RideSharingServer is starting on port " + PORT);

        // Initialize Database Connection
        DatabaseManager dbParams = new DatabaseManager();

        // Periodic Task to Sync Map Data (Every 2 seconds)
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            dbParams.exportDataToJSON("web/data.json");
        }, 0, 2, TimeUnit.SECONDS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                pool.execute(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
