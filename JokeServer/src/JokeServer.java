import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
        try {
            server.start("localhost", Integer.parseInt(args[0]));
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number, using default port: " + PORT);
            server.start("localhost", PORT);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("no port specified, using default port: " + PORT);
            server.start("localhost", PORT);
        }
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

class ClientHandler extends Thread {
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
