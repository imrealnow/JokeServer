import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class JokeClient {
    Scanner scanner;
    Socket clientSocket;
    DataInputStream serverOutput;
    DataOutputStream clientInput;

    final String EXIT_COMMAND = "N";
    boolean running = false;
    CompletableFuture<Boolean> disconnected = new CompletableFuture<>();

    public static void main(String[] args) throws Exception {
        JokeClient client = new JokeClient();
        client.attemptConnection();
        client.run();
    }

    public CompletableFuture<Boolean> getDisconnected() {
        return disconnected;
    }

    /**
     * Starts a connection to the server.
     * 
     * @param host the server's hostname
     * @param port the server's port number
     * @throws IOException              if the connection fails
     * @throws IllegalArgumentException if the port number is invalid
     * @throws UnknownHostException     if the hostname is invalid
     */
    public void startConnection(String host, int port)
            throws UnknownHostException, IOException, IllegalArgumentException {
        scanner = new Scanner(System.in);
        clientSocket = new Socket(host, port);
        serverOutput = new DataInputStream(clientSocket.getInputStream());
        clientInput = new DataOutputStream(clientSocket.getOutputStream());
    }

    /**
     * Asks the user for the server's hostname and port number.
     * Then attempts to connect to the server. If the connection fails,
     * the user is asked to try again.
     */
    private void attemptConnection() {
        scanner = new Scanner(System.in);
        boolean connected = false;
        String host = "";
        while (!connected) {
            try {
                System.out.println("Enter the server's IP address: ");
                host = scanner.nextLine().trim();
                startConnection(host, JokeServer.PORT);
                connected = true;
            } catch (UnknownHostException e) {
                System.out.println("Unknown host: " + e.getMessage());
            } catch (IOException e) {
                System.out.println(host);
                System.out.println("Couldn't get I/O for the connection to: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Port Number: " + e.getMessage());
            }
        }
    }

    /**
     * Gets output from the server and sends input back to the server.
     */
    public void run() {
        running = true;
        while (running) {
            try {
                // get output from server
                String response = serverOutput.readUTF();
                System.out.println(response);

                // send input to server
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase(EXIT_COMMAND)) {
                    System.out.println("Connection closing: " + clientSocket);
                    running = false;
                } else {
                    clientInput.writeUTF(input);
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        // close connection and clean up resources
        try {
            disconnect();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }

    public void disconnect() throws IOException {
        scanner.close();
        serverOutput.close();
        clientInput.close();
        disconnected.complete(true);
        System.out.println("Closed");
    }

    /**
     * Sends a message to the server, receives and then returns the response.
     * 
     * @param msg the message to send to the server
     * @return the response from the server
     */
    public String sendMessage(String msg) throws IOException {
        clientInput.writeUTF(msg);
        String response = serverOutput.readUTF();
        System.out.println(response);
        return response;
    }

    /**
     * Reads current server output and returns it.
     * 
     * @return current server output
     * @throws IOException Data stream has been closed
     */
    public String readMessage() throws IOException {
        String response = serverOutput.readUTF();
        System.out.println(response);
        return response;
    }
}
