import java.io.*;
import java.net.*;
import java.util.*;

public class JokeClient {
    Scanner scanner;
    Socket clientSocket;
    DataInputStream serverOutput;
    DataOutputStream clientInput;

    final String EXIT_COMMAND = "N";
    boolean running = false;

    public JokeClient() {
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) throws Exception {
        JokeClient client = new JokeClient();
        client.attemptConnection();
        client.run();
    }

    /**
     * Asks the user for the server's hostname and port number.
     * Then attempts to connect to the server. If the connection fails,
     * the user is asked to try again.
     */
    private void attemptConnection() {
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
            scanner.close();
            serverOutput.close();
            clientInput.close();
            System.out.println("Closed");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }

    public void startConnection(String host, int port)
            throws UnknownHostException, IOException, IllegalArgumentException {
        clientSocket = new Socket(host, port);
        serverOutput = new DataInputStream(clientSocket.getInputStream());
        clientInput = new DataOutputStream(clientSocket.getOutputStream());
    }

    public String sendMessage(String msg) throws Exception {
        clientInput.writeUTF(msg);
        String response = serverOutput.readUTF();
        System.out.println(response);
        return response;
    }

    public String readMessage() throws Exception {
        String response = serverOutput.readUTF();
        System.out.println(response);
        return response;
    }
}
