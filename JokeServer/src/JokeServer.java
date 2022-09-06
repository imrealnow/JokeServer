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

class JokeClient {
    Scanner scanner;
    Socket clientSocket;
    DataInputStream serverOutput;
    DataOutputStream clientInput;

    final String EXIT_COMMAND = "N";
    boolean running = false;
    CompletableFuture<Boolean> disconnected = new CompletableFuture<>();

    public static void main(String[] args) throws Exception {
        JokeClient client = new JokeClient();
        client.attemptConnection(args[0], Integer.parseInt(args[1]));
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
    private void attemptConnection(String host, int port) {
        scanner = new Scanner(System.in);
        boolean connected = false;
        while (!connected) {
            try {
                startConnection(host, port);
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
