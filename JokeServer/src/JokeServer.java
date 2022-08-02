import java.io.*;
import java.net.*;
import java.util.*;

public class JokeServer {
    public static final int PORT = 4444;
    public static final String[] JOKE_STRINGS = new String[] {
            "What did the cheese say when it looked in the mirror?\n\"Hello-me (Halloumi)\"\n",
            "What kind of cheese do you use to disguise a small horse?\nWhatever kind of cheese is big enough to fit a small horse inside\n",
            "Why did the doctor get fired from his job?\nHe lost his patience(Patients).\n",
            "what do you call a fake noodle? An Impasta.\n"
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

    /**
     * Listens for clients and creates a new thread for each one.
     * 
     * @throws IOException if an I/O error occurs when waiting for a connection.
     */
    public void listenForClients() throws IOException {
        running = true;
        while (running) {
            Socket clientSocket = serverSocket.accept();
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            ClientHandler client = new ClientHandler(this, clients.size(), clientSocket, in, out);
            clients.add(client);
            client.start();
        }
    }

    public void disconnect() throws Exception {
        running = false;
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
