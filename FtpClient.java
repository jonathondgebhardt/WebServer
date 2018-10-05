
/**
 * Assignment 1
 * Jonathon Gebhardt
 **/

import java.io.*;
import java.net.*;
import java.util.regex.*;

public class FtpClient {

    final static String CRLF = "\r\n";
    private boolean FILEZILLA = false; // Enabled if connecting to FILEZILLA Server
    private boolean DEBUG = false; // Debug Flag
    private Socket controlSocket = null;
    private BufferedReader controlReader = null;
    private DataOutputStream controlWriter = null;
    private String currentResponse;

    private final static int OK_RESPONSE = 220;
    private final static int USER_OK = 331;
    private final static int LOGIN_OK = 230;
    private final static int FILE_ACTION_OK = 250;
    private final static int ENTERING_PASSIVE = 227;
    private final static int FILE_STATUS_OK = 150;
    private final static int FILE_NOT_FOUND = 550;

    /*
     * Constructor
     */
    public FtpClient() {
    }

    /*
     * Connect to the FTP server
     *
     * @param username: the username you use to login to your FTP session
     *
     * @param password: the password associated with the username
     */
    public void connect(String username, String password) {
        try {
            // Establish the control socket.
            this.controlSocket = new Socket("127.0.0.1", 21);

            // Get references to the socket input and output streams.
            this.controlReader = new BufferedReader(new InputStreamReader(this.controlSocket.getInputStream()));
            this.controlWriter = new DataOutputStream(this.controlSocket.getOutputStream());

            // Check if the initial connection response code is OK.
            if (checkResponse(OK_RESPONSE)) {
                System.out.println("Succesfully connected to FTP server");
            }

            if (FILEZILLA) {
                for (int i = 0; i < 2; i++) {
                    this.currentResponse = this.controlReader.readLine();
                    if (DEBUG) {
                        System.out.println("Current FTP response: " + this.currentResponse);
                    }
                }
            }

            // Send user name and password to ftp server. It's OK if we don't have a
            // successful login here due to the logic in HttpRequest.
            sendCommand("USER " + username + CRLF, USER_OK);
            sendCommand("PASS " + password + CRLF, LOGIN_OK);

        } catch (UnknownHostException ex) {
            System.out.println("UnknownHostException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    /*
     * Retrieve the file from FTP server after connection is established
     *
     * @param file_name: the name of the file to retrieve
     */
    public void getFile(String file_name) {
        // Initialize the data port.
        int data_port = 0;

        try {
            // Change to current (root) directory first.
            sendCommand("CWD ~" + CRLF, FILE_ACTION_OK);

            // Set to passive mode and retrieve the data port number from response. If the
            // login is unsuccessful, we will not be able to extract the data port.
            this.currentResponse = sendCommand("PASV" + CRLF, ENTERING_PASSIVE);
            data_port = extractDataPort(this.currentResponse);

            // Connect to the data port.
            Socket data_socket = new Socket("127.0.0.1", data_port);
            DataInputStream data_reader = new DataInputStream(data_socket.getInputStream());

            // Download file from ftp server.
            this.currentResponse = sendCommand("RETR " + file_name + CRLF, FILE_STATUS_OK);

            // Check if the transfer was succesful. Not using checkResponse here because
            // vsftpd does not send another response if the file doesn't exist. Thread
            // blocks on checkResponse otherwise.
            if (!this.currentResponse.startsWith(String.valueOf(FILE_NOT_FOUND))) {
                // Write data on a local file
                createLocalFile(data_reader, file_name);
            }

            data_socket.close();

        } catch (UnknownHostException ex) {
            System.out.println("UnknownHostException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex);
        }
    }

    /*
     * Close the FTP connection
     */
    public void disconnect() {
        try {
            this.controlReader.close();
            this.controlWriter.close();
            this.controlSocket.close();
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    /*
     * Send ftp command
     *
     * @param command: the full command line to send to the ftp server
     *
     * @param expected_code: the expected response code from the ftp server
     *
     * @return the response line from the ftp server after sending the command
     */
    private String sendCommand(String command, int expected_response_code) {
        String response = "";
        try {
            // Send command to the ftp server.
            this.controlWriter.writeBytes(command);

            // Get response from ftp server.
            response = this.controlReader.readLine();
            if (DEBUG) {
                System.out.println("Current FTP response: " + response);
            }

            // Check validity of response.
            if (!response.startsWith(String.valueOf(expected_response_code))) {
                throw new IOException("Bad response: " + response);
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }

        return response;
    }

    /*
     * Check the validity of the ftp response, the response code should correspond
     * to the expected response code
     *
     * @param expected_code: the expected ftp response code
     *
     * @return response status: true if successful code
     */
    private boolean checkResponse(int expected_code) {
        boolean response_status = true;

        try {
            this.currentResponse = this.controlReader.readLine();

            if (DEBUG) {
                System.out.println("Current FTP response: " + this.currentResponse);
            }

            if (!this.currentResponse.startsWith(String.valueOf(expected_code))) {
                response_status = false;
                throw new IOException("Bad response: " + this.currentResponse);
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        return response_status;
    }

    /*
     * Given the complete ftp response line of setting data transmission mode to
     * passive, extract the port to be used for data transfer
     *
     * @param response_line: the ftp response line
     *
     * @return the data port number
     */
    private int extractDataPort(String response_line) {
        int data_port = 0;
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(response_line);
        String[] str = new String[6];

        if (matcher.find()) {
            str = matcher.group(1).split(",");
        }

        if (DEBUG) {
            System.out.println("Port integers: " + str[4] + "," + str[5]);
        }

        data_port = Integer.valueOf(str[4]) * 256 + Integer.valueOf(str[5]);

        if (DEBUG) {
            System.out.println("Data Port: " + data_port);
        }

        return data_port;
    }

    /*
     * Create the file locally after retreiving data over the FTP data stream.
     *
     * @param dis: the data input stream
     *
     * @param file_name: the name of the file to create
     */
    private void createLocalFile(DataInputStream dis, String file_name) {
        byte[] buffer = new byte[1024];

        int bytes = 0;
        try {
            FileOutputStream fos = new FileOutputStream(new File(file_name));

            while ((bytes = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytes);
            }

            dis.close();
            fos.close();
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException" + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }
}
