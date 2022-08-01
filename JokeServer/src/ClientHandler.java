import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {
    JokeServer server;
    Socket clientSocket;
    DataInputStream in;
    DataOutputStream out;
    boolean running = true;

    public ClientHandler(JokeServer server, Socket clientSocket, DataInputStream in, DataOutputStream out) {
        this.server = server;
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        while (running) {
            try {
                out.writeUTF("Do you want to hear a joke? Y/N");
                String response = in.readUTF().toUpperCase();
                handleInput(response);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        try {
            out.close();
            in.close();
            clientSocket.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleInput(String input) {
        try {
            switch (input) {
                case "Y":
                    out.writeUTF(server.getRandomJoke());
                    break;
                case "N":
                    out.writeUTF("Bye!");
                    break;
                default:
                    out.writeUTF("Invalid input!");
                    break;
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void stopHandler() {
        running = false;
    }
}
