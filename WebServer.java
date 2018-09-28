import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer {
  public static void main(String[] args) {

    int port = 1234;

    ServerSocket serverSocket = null;
    Socket socket = null;

    try {
      serverSocket = new ServerSocket(port);

      while (true) {
        socket = serverSocket.accept();

        // Create new HTTP request
        HttpRequest request = new HttpRequest(socket);
        Thread thread = new Thread(request);
        thread.start();
      }

    } catch (UnknownHostException e) {
      System.err.println("Error -- Could not resolve host in server");
    } catch (Exception e) {
      System.err.println("Error -- Generic exception in server");
    }
  }
}
