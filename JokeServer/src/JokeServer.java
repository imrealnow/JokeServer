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
    List<String> consoleLog = new ArrayList<String>();
    List<Runnable> disconnectListeners = new ArrayList<Runnable>();
    boolean running = false;
    int clientCount;

    public static void main(String[] args) throws Exception {
        JokeServer server = new JokeServer();
        server.start("localhost", PORT);
        server.listenForClients();
    }

    public List<String> getConsoleLog() {
        return Collections.unmodifiableList(consoleLog);
    }

    public void registerDisconnectListener(Runnable listener) {
        disconnectListeners.add(listener);
    }

    public void deregisterDisconnectListener(Runnable listener) {
        disconnectListeners.remove(listener);
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
        printToConsole("=== JokeServer running on:" + getPublicIPAddress() + ":" + PORT + " ===");
        running = true;
        while (running) {
            Socket clientSocket = serverSocket.accept();
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            int clientId = clientCount++;
            ClientHandler client = new ClientHandler(this, clientId, clientSocket, in, out);
            clients.add(client);
            client.start();
            printToConsole("[Client " + clientId + " connected: " + clientSocket.getInetAddress() + ":"
                    + clientSocket.getPort() + "]");
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

    public void disconnectClient(int clientId) throws Exception {
        // update observers
        for (Runnable listener : disconnectListeners) {
            listener.run();
        }
        ClientHandler client;
        for (int i = 0; i < clients.size(); i++) {
            client = clients.get(i);
            if (client.getId() == clientId) {
                printToConsole("[Client " + clientId + " disconnected: " + client.getSocket().getInetAddress() + ":"
                        + client.getSocket().getPort() + "]");
                clients.remove(i);
                client.stopHandler();
                break;
            }
        }
    }

    private void printToConsole(String msg) {
        System.out.println(msg);
        consoleLog.add(msg);
    }

    public String getRandomJoke() {
        int randomIndex = (int) (Math.random() * JOKE_STRINGS.length);
        return JOKE_STRINGS[randomIndex];
    }

    /**
     * Reads public IP address of the server by checking
     * amazon's webpage.
     * 
     * @return
     * @throws IOException
     */
    public String getPublicIPAddress() throws IOException {
        // Credit: https://www.baeldung.com/java-get-ip-address
        String urlString = "http://checkip.amazonaws.com/";
        URL url = new URL(urlString);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.readLine();
        }
    }
}
