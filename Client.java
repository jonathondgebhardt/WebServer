import java.io.*;
import java.net.*;

public class Client {

    public static void main(String[] args) {

        String address = "127.0.0.1";
        int port = 1234;

        Socket socket = null;
        DataInputStream input = null;
        DataOutputStream out = null;
        // Create a socket to the server.
        try {
            socket = new Socket(address, port);

            // takes input from terminal
            input = new DataInputStream(System.in);

            // sends output to the socket
            out = new DataOutputStream(socket.getOutputStream());

            socket.close();
        } catch (UnknownHostException e) {
            System.err.println("Error -- Could not resolve host in client");
        } catch (IOException e) {
            System.err.println(e);
        } catch (Exception e) {
            System.err.println("Error -- Generic exception in client");
        }
    }
}
