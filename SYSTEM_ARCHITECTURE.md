# Distributed Ride-Sharing System Architecture

## 1. Who is the "Server"?
In this Distributed System, there is no single "Server". Instead, the system is composed of four specialized servers working together (Microservices Architecture).

### core Components:
1.  **Web Gateway Server (Port 8080)** - `WebGatewayServer.java`
    *   **Role**: The "Front Door". It is the only server visible to the public (Browsers/Apps).
    *   **Function**: Serves the website (HTML/JS) and acts as a **Reverse Proxy**, forwarding API requests to the internal backed servers.

2.  **Dispatch Server (Port 5000)** - `DispatchServer.java`
    *   **Role**: The "Brain".
    *   **Function**: Matching passengers with drivers, calculating distances, and managing ride states.

3.  **Driver Service (Port 5001)** - `DriverServiceServer.java`
    *   **Role**: The "Tracker".
    *   **Function**: Maintains real-time connections with drivers to track their location and availability.

4.  **Database Service (Port 5002)** - `DatabaseServiceServer.java`
    *   **Role**: The "Vault".
    *   **Function**: The only valid way to access the MySQL database. Other services must ask it to read/write data.

---

## 2. How to Differentiate?
Differentiation is achieved by **Functional Decomposition**. Each server has a specific responsibility:
*   **Need to change the UI?** Update `WebGatewayServer` (or the HTML files it serves).
*   **Need to update Ride Logic?** Update `DispatchServer`.
*   **Need to change Database?** Update `DatabaseServiceServer`.

This separation allows you to modify one part of the system without breaking the others (Loose Coupling).

---

## 3. How to Multiply (Scale) Servers?
"Multiplying" your servers is known as **Horizontal Scaling**. If the Dispatch Server gets too busy, you can run multiple copies of it.

### Step-by-Step Guide to Scaling:

#### A. Run Multiple Instances
Modify your startup script to launch multiple Dispatch Servers on different ports:
```bat
start java -cp "lib/*;." services.dispatch.DispatchServer 5000
start java -cp "lib/*;." services.dispatch.DispatchServer 5005
start java -cp "lib/*;." services.dispatch.DispatchServer 5006
```
*(Note: You will need to modify `DispatchServer.java` main method to accept a port argument)*

#### B. Implement Load Balancing
Update the **Web Gateway Server** to know about these new ports. Instead of hardcoding 5000, use a list:

```java
// In WebGatewayServer.java
private static final List<Integer> DISPATCH_PORTS = Arrays.asList(5000, 5005, 5006);
private static int currentPortIndex = 0;

private static synchronized int getNextDispatchPort() {
    int port = DISPATCH_PORTS.get(currentPortIndex);
    currentPortIndex = (currentPortIndex + 1) % DISPATCH_PORTS.size(); // Round-Robin
    return port;
}
```

#### C. Centralize State (Advanced)
Currently, `DispatchServer` keeps ride requests in memory (`activePassengers` HashMap). If you run multiple servers, Server A won't know about Server B's rides.
*   **Solution**: Move the "State" (Active Rides Map) to a shared storage like **Redis** or rely entirely on the **Database Service** for state.

By following these steps, you can multiply your servers to handle thousands of users!
