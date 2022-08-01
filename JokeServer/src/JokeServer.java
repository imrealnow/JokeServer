import java.io.*;
import java.net.*;
import java.util.*;

public class JokeServer {
    public static final int PORT = 4444;
    static final String[] JOKE_STRINGS = new String[] {
            "What did the cheese say when it looked in the mirror? \"Hello-me (Halloumi)\"",
            "What kind of cheese do you use to disguise a small horse? Whatever kind of cheese is big enough to fit a small horse inside",
            "Why did the doctor get fired from his job? He lost his patience(Patients).",
            "what do you call a fake noodle? An Impasta."
    };

    ServerSocket serverSocket;
    List<ClientHandler> clients = new ArrayList<ClientHandler>();
    boolean running = false;

    public static void main(String[] args) throws Exception {
        JokeServer server = new JokeServer();
        server.start("localhost", PORT);
        server.listenForClients();
    }

    public void start(String host, int port) throws Exception {
        serverSocket = new ServerSocket(port);
    }

    public void listenForClients() throws Exception {
        running = true;
        while (running) {
            Socket clientSocket = serverSocket.accept();
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            ClientHandler client = new ClientHandler(this, clientSocket, in, out);
            clients.add(client);
            client.start();
        }
    }

    public void disconnect() throws Exception {
        for (ClientHandler client : clients) {
            client.stopHandler();
        }
        clients.clear();
        serverSocket.close();
    }

    public String getRandomJoke() {
        int randomIndex = (int) (Math.random() * JOKE_STRINGS.length);
        return JOKE_STRINGS[randomIndex];
    }
}
