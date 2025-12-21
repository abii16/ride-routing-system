package services.gateway;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import common.*;

public class DebugGateway {
  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
    server.createContext("/api/driver/register", new WebGatewayServer.DriverRegistrationHandler());
    server.setExecutor(null);
    server.start();
    System.out.println("Debug Gateway started on 8081");
  }
}
