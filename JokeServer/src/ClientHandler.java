import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {
    JokeServer server;
    int clientId;
    Socket clientSocket;
    DataInputStream in;
    DataOutputStream out;
    boolean running = true;

    public ClientHandler(JokeServer server, int clientId, Socket clientSocket, DataInputStream in,
            DataOutputStream out) {
        this.server = server;
        this.clientId = clientId;
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
    }

    public int getClientId() {
        return clientId;
    }

    public Socket getSocket() {
        return clientSocket;
    }

    @Override
    public void run() {
        // welcome message
        String localIp;
        try {
            localIp = (InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            localIp = "(Couldn't retrieve local IP address)";
        }
        String message = "[Client: " + clientId + "] has connected to Joke Server["
                + localIp + ":" + server.serverSocket.getLocalPort()
                + "]";
        while (running) {
            try {
                message += "\nDo you want to hear a joke? (Y/N)";
                out.writeUTF(message);
                String response = in.readUTF().toUpperCase();
                message = handleInput(response);
            } catch (Exception e) {
                // client disconnected
                running = false;
            }
        }
        try {
            server.disconnectClient(clientId);
            out.close();
            in.close();
        } catch (Exception e) {
            System.out.println("Handler close Error: " + e.getMessage());
        }
    }

    private String handleInput(String input) {
        switch (input) {
            case "Y":
                return server.getRandomJoke();
            case "N":
                return "Bye!";
            default:
                return "Invalid input!";
        }
    }

    public void stopHandler() {
        running = false;
    }
}
