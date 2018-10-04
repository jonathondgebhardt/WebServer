// package webserver;

/**
 * Assignment 1
 * Jonathon Gebhardt
 **/

import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer {
  public static void main(String[] args) {

    int port = 6789;

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

final class HttpRequest implements Runnable {
  private final static String CRLF = "\r\n";
  private Socket socket;

  public HttpRequest(Socket socket) throws Exception {
    this.socket = socket;
  }

  public void run() {
    try {
      processRequest();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void processRequest() throws Exception {
    // Get a reference to the socket's input and output streams.
    BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    DataOutputStream os = new DataOutputStream(this.socket.getOutputStream());

    // Get the request line of the HTTP request message.
    String requestLine = br.readLine();

    // Print request to console.
    System.out.println("Request:\n------------\n" + requestLine);

    // Get and display the header lines.
    String headerLine = null;
    while ((headerLine = br.readLine()).length() != 0) {
      System.out.println(headerLine);
    }

    // Tokenize request line.
    StringTokenizer tokens = new StringTokenizer(requestLine);

    // Skip command.
    tokens.nextToken();

    // Prepend a "." so that file request is within the current directory.
    String fileName = tokens.nextToken();
    fileName = "." + fileName;

    String version = tokens.nextToken();

    // Open the requested file.
    FileInputStream fis = null;
    boolean fileExists = true;
    try {
      fis = new FileInputStream(fileName);
    } catch (FileNotFoundException e) {
      fileExists = false;
    }

    // Construct the response message.
    String statusLine = null;
    String contentTypeLine = null;
    String entityBody = null;

    if (fileExists) {
      // File exists on the server.
      statusLine = version + " 200 OK" + CRLF;
      contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
    } else {
      // File doesn't exist on the server. Initializing response message with 404 in
      // case file is not on ftp server either.
      statusLine = version + " 404 Not Found" + CRLF;
      contentTypeLine = "Content-type: (text/html)" + CRLF;
      entityBody = "<HTML><HEAD><TITLE>Not Found</TITLE></HEAD><BODY>Not Found</BODY></HTML>";

      if (contentType(fileName).equalsIgnoreCase("(text/plain)")) {
        // Attempting to get .txt file from FTP server. Create an instance of ftp
        // client.
        FtpClient ftp = new FtpClient();

        // Connect to the ftp server.
        ftp.connect("jon", "Vibrato2019$");

        // Retrieve the file from the ftp server. Remember you need to
        // first upload this file to the ftp server under your user ftp directory.
        ftp.getFile(fileName);

        // Disconnect from ftp server.
        ftp.disconnect();

        try {
          // Assign input stream to read the recently ftp-downloaded file. If the file
          // could not be retrieved from the ftp server, response message is already
          // initialized.
          fis = new FileInputStream(fileName);

          statusLine = version + " 200 OK" + CRLF;
          contentTypeLine = "Content-type: (text/plain)" + CRLF;

          fileExists = true;
        } catch (FileNotFoundException e) {
        }
      }
    }

    // Send the status line.
    os.writeBytes(statusLine);
    // Send the content type line.
    os.writeBytes(contentTypeLine);
    // Send a blank line to indicate the end of the header lines.
    os.writeBytes(CRLF);

    // Send the entity body.
    if (fileExists) {
      try {
        sendBytes(fis, os);
      } catch (Exception e) {
        System.err.println("Could not send file to client.");
      }
      fis.close();
    } else {
      os.writeBytes(entityBody);
    }

    String serverResponse = statusLine + "\n" + contentTypeLine;
    System.out.println("\nResponse:\n-------------\n" + serverResponse + "\n");

    // Close streams and socket.
    os.close();
    br.close();
    socket.close();
  }

  private String contentType(String fileName) {
    if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
      return "(text/html)";
    }
    if (fileName.endsWith(".txt")) {
      return "(text/plain)";
    }
    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
      return "(image/jpeg)";
    }
    if (fileName.endsWith(".png")) {
      return "(image/png)";
    }
    if (fileName.endsWith(".gif")) {
      return "(image/gif)";
    }

    return "(application/octet-stream)";
  }

  private void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
    // Construct a 1K buffer to hold bytes on their way to the socket.
    byte[] buffer = new byte[1024];
    int bytes = 0;
    // Copy requested file into the socket's output stream.
    while ((bytes = fis.read(buffer)) != -1) {
      os.write(buffer, 0, bytes);
    }
  }
}
