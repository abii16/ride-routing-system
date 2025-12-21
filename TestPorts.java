import java.net.Socket;

public class TestPorts {
  public static void main(String[] args) {
    int[] ports = { 8080, 5000, 5001, 5002, 5003 };
    String[] names = { "Web Gateway", "Dispatch", "Driver (Direct)", "Database", "Driver API" };

    for (int i = 0; i < ports.length; i++) {
      try (Socket s = new Socket("localhost", ports[i])) {
        System.out.println("Port " + ports[i] + " (" + names[i] + "): OPEN");
      } catch (Exception e) {
        System.out.println("Port " + ports[i] + " (" + names[i] + "): CLOSED");
      }
    }
  }
}
