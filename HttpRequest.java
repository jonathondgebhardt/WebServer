import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Class: CEG4400-01 Assignment: Project 01 Instructor: Dr. Bin Wang TA: Steve
 * Betts
 *
 * @author Jonathon Gebhardt
 */

public class HttpRequest implements Runnable {
  private final static String CRLF = "\r\n";
  private Socket socket;

  public HttpRequest(Socket socket) throws Exception {
    this.socket = socket;
  }

  public void run() {
    try {
      processRequest();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  private void processRequest() throws Exception {
    // Get a reference to the socket's input and output streams.
    InputStreamReader is = new InputStreamReader(this.socket.getInputStream());
    DataOutputStream os = new DataOutputStream(this.socket.getOutputStream());

    // Set up input stream filters.
    BufferedReader br = new BufferedReader(is);

    // Get the request line of the HTTP request message.
    // Request-Line = Method SP Request-URI SP HTTP-Version CRLF
    String requestLine = br.readLine();

    // Tokenize request line
    StringTokenizer tokens = new StringTokenizer(requestLine);

    // Skip command
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
      statusLine = version + " 200 Accepted";
      contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
    } else {
      if (!contentType(fileName).equalsIgnoreCase(?)) {
        statusLine = ?;
        contentTypeLine = ?;
        entityBody = "<HTML>" +
        "<HEAD><TITLE>Not Found</TITLE></HEAD>" +
        "<BODY>Not Found</BODY></HTML>";
      } else { // else retrieve the text (.txt) file from your local FTP server
        statusLine = version + " 404 Not Found";

        // https://github.com/sockjs/sockjs-protocol/issues/17
        contentTypeLine = "text/plain; charset=UTF-8";
  
        entityBody = "<HTML><HEAD><TITLE>Not Found</TITLE></HEAD><BODY>Not Found</BODY></HTML>";
        // disconnect from ftp server
        // assign input stream to read the recently ftp-downloaded file
        fis = new FileInputStream(fileName);
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
      System.out.println("Sending 404 entity.");
      os.writeBytes(entityBody);
    }

    // Close streams and socket.
    os.close();
    br.close();
    socket.close();
  }

  private static String contentType(String fileName) {
    if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
      return "text/html";
    }
    // if(?) {
    // ?;
    // }
    // if(?) {
    // ?;
    // }

    return "application/octet-stream";
  }

  private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
    // Construct a 1K buffer to hold bytes on their way to the socket.
    byte[] buffer = new byte[1024];
    int bytes = 0;
    // Copy requested file into the socket's output stream.
    while ((bytes = fis.read(buffer)) != -1) {
      os.write(buffer, 0, bytes);
    }
  }
}
